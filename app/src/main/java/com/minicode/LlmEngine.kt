package com.minicode

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

class LlmEngine(context: Context) {

    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var modelLoaded = false

    init {
        System.loadLibrary("minicode")
    }

    fun loadModel(path: String): Boolean {
        modelLoaded = nativeLoadModel(path)
        return modelLoaded
    }

    fun generate(
        instruction: String,
        fileContent: String,
        callback: (Result<String>) -> Unit
    ) {
        if (!modelLoaded) {
            callback(Result.failure(IllegalStateException("Model not loaded")))
            return
        }
        val fullPrompt = buildPrompt(instruction, fileContent)
        if (fullPrompt.length > Constants.MAX_INPUT_CHARS) {
            callback(Result.failure(IllegalArgumentException("Input too long")))
            return
        }
        executor.execute {
            try {
                val result = nativeGenerate(
                    fullPrompt,
                    Constants.MAX_OUTPUT_TOKENS,
                    0.1f,
                    0.9f,
                    1.1f,
                    42
                )
                val output = result?.trim() ?: ""
                val finalResult = when {
                    output == Constants.ERROR_TOO_COMPLEX ||
                        (output.contains(Constants.ERROR_TOO_COMPLEX) &&
                            output.lines().count { it.isNotBlank() } <= 1) ->
                        Result.failure<String>(IllegalStateException(Constants.ERROR_TOO_COMPLEX))
                    else ->
                        Result.success(output)
                }
                mainHandler.post { callback(finalResult) }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    private fun buildPrompt(instruction: String, fileContent: String): String {
        val prefix = "${Constants.SYSTEM_PROMPT}\n\nInstruction: $instruction\n\nCurrent file content:\n"
        val maxContentLen = Constants.MAX_INPUT_CHARS - prefix.length
        val content = if (fileContent.length > maxContentLen) {
            fileContent.take(maxContentLen) + "\n..."
        } else {
            fileContent
        }
        return prefix + content
    }

    fun isLoaded(): Boolean = modelLoaded

    private external fun nativeLoadModel(path: String): Boolean
    private external fun nativeGenerate(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        repeatPenalty: Float,
        seed: Int
    ): String?
}
