package com.minicode

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LlmEngine(context: Context) {

    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val modelLoaded = AtomicBoolean(false)
    private val generationInProgress = AtomicBoolean(false)

    @Volatile
    private var currentCallback: StreamCallback? = null

    init {
        System.loadLibrary("minicode")
    }

    fun loadModel(path: String): Boolean {
        val ok = nativeLoadModel(path)
        modelLoaded.set(ok)
        return ok
    }

    fun unloadModel() {
        nativeUnloadModel()
        modelLoaded.set(false)
    }

    fun isLoaded(): Boolean = modelLoaded.get()

    /**
     * Start streaming generation. Runs off main thread. onToken is posted to main thread.
     * completion is invoked on main thread with full output or failure (including cancel).
     * If a generation is already running, completion is invoked with failure and this returns.
     */
    fun generateStream(
        instruction: String,
        fileContent: String,
        useCamelCase: Boolean,
        onToken: (String) -> Unit,
        completion: (Result<String>) -> Unit
    ) {
        if (!modelLoaded.get()) {
            mainHandler.post { completion(Result.failure(IllegalStateException("Model not loaded"))) }
            return
        }
        if (!generationInProgress.compareAndSet(false, true)) {
            mainHandler.post { completion(Result.failure(IllegalStateException("Generation already in progress"))) }
            return
        }
        val fullPrompt = buildPrompt(instruction, fileContent, useCamelCase)
        if (fullPrompt.length > Constants.MAX_INPUT_CHARS) {
            generationInProgress.set(false)
            mainHandler.post { completion(Result.failure(IllegalArgumentException("Input too long"))) }
            return
        }
        val output = StringBuilder()
        val callback = StreamCallback { token ->
            output.append(token)
            mainHandler.post { onToken(token) }
        }
        currentCallback = callback
        executor.execute {
            try {
                nativeGenerateStreaming(
                    fullPrompt,
                    Constants.MAX_OUTPUT_TOKENS,
                    0.1f,
                    0.9f,
                    1.1f,
                    42,
                    callback
                )
                val fullOutput = output.toString().trim()
                val cancelled = callback.cancelled
                mainHandler.post {
                    currentCallback = null
                    generationInProgress.set(false)
                    when {
                        cancelled -> completion(Result.failure(java.util.concurrent.CancellationException("Cancelled")))
                        fullOutput == Constants.ERROR_TOO_COMPLEX ||
                            (fullOutput.contains(Constants.ERROR_TOO_COMPLEX) &&
                                fullOutput.lines().count { it.isNotBlank() } <= 1) ->
                            completion(Result.failure(IllegalStateException(Constants.ERROR_TOO_COMPLEX)))
                        else -> completion(Result.success(fullOutput))
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    currentCallback = null
                    generationInProgress.set(false)
                    completion(Result.failure(e))
                }
            }
        }
    }

    fun cancelGeneration() {
        currentCallback?.cancelled = true
    }

    fun isGenerating(): Boolean = generationInProgress.get()

    private fun buildPrompt(instruction: String, fileContent: String, useCamelCase: Boolean): String {
        var prefix = "${Constants.SYSTEM_PROMPT}\n\nInstruction: $instruction\n\n"
        if (useCamelCase) {
            prefix += "Use CamelCase for variable names.\n\n"
        }
        prefix += "Current file content:\n"
        val maxContentLen = Constants.MAX_INPUT_CHARS - prefix.length
        val content = if (fileContent.length > maxContentLen) {
            fileContent.take(maxContentLen) + "\n..."
        } else {
            fileContent
        }
        return prefix + content
    }

    private external fun nativeLoadModel(path: String): Boolean
    private external fun nativeUnloadModel()
    private external fun nativeGenerateStreaming(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        repeatPenalty: Float,
        seed: Int,
        callback: StreamCallback
    )
}
