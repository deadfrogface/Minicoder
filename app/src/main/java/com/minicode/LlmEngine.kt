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
     * Safety guards: line repetition (3x), token sequence repetition (10+), max chars, max time.
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
        var lastGeneratedLine: String? = null
        var previousGeneratedLine: String? = null
        var repetitionCount = 0
        val tokenBuffer = ArrayDeque<String>(Constants.TOKEN_SAFETY_BUFFER_SIZE)
        val startTimeMs = System.currentTimeMillis()

        var callback: StreamCallback? = null
        callback = StreamCallback { token ->
            val cb = callback!!
            if (cb.cancelled) return@StreamCallback
            output.append(token)
            mainHandler.post { onToken(token) }

            // Hard limit: max output chars
            if (output.length >= Constants.MAX_OUTPUT_CHARS) {
                cb.cancelled = true
                cb.cancellationReason = Constants.CANCELLATION_REASON_SAFETY_LIMIT
                return@StreamCallback
            }
            // Hard limit: max generation time
            if (System.currentTimeMillis() - startTimeMs >= Constants.MAX_GENERATION_TIME_MS) {
                cb.cancelled = true
                cb.cancellationReason = Constants.CANCELLATION_REASON_SAFETY_LIMIT
                return@StreamCallback
            }

            // Line repetition: same full line 3 times consecutively
            val lines = output.toString().split('\n')
            if (lines.size >= 2) {
                val lastCompleteLine = lines[lines.size - 2].trim()
                if (lastCompleteLine.isNotEmpty()) {
                    when {
                        lastCompleteLine == lastGeneratedLine -> {
                            repetitionCount++
                            if (repetitionCount >= 3) {
                                cb.cancelled = true
                                cb.cancellationReason = Constants.CANCELLATION_REASON_SAFETY_LIMIT
                                return@StreamCallback
                            }
                        }
                        else -> {
                            previousGeneratedLine = lastGeneratedLine
                            lastGeneratedLine = lastCompleteLine
                            repetitionCount = 1
                        }
                    }
                }
            }

            // Token-level repetition: same sequence of 10+ tokens repeats consecutively
            tokenBuffer.addLast(token)
            if (tokenBuffer.size > Constants.TOKEN_SAFETY_BUFFER_SIZE) tokenBuffer.removeFirst()
            if (tokenBuffer.size >= 2 * Constants.TOKEN_SAFETY_REPEAT_LEN) {
                val list = tokenBuffer.toList()
                for (n in Constants.TOKEN_SAFETY_REPEAT_LEN..minOf(Constants.TOKEN_SAFETY_BUFFER_SIZE / 2, list.size / 2)) {
                    val tail = list.takeLast(n)
                    val prev = list.dropLast(n).takeLast(n)
                    if (tail == prev) {
                        cb.cancelled = true
                        cb.cancellationReason = Constants.CANCELLATION_REASON_SAFETY_LIMIT
                        return@StreamCallback
                    }
                }
            }
        }
        currentCallback = callback
        val cb = callback!!
        executor.execute {
            try {
                nativeGenerateStreaming(
                    fullPrompt,
                    Constants.MAX_OUTPUT_TOKENS,
                    0.2f,
                    40,
                    0.9f,
                    1.18f,
                    128,
                    42,
                    cb
                )
                val fullOutput = output.toString().trim()
                val cancelled = cb.cancelled
                val reason = cb.cancellationReason
                mainHandler.post {
                    currentCallback = null
                    generationInProgress.set(false)
                    when {
                        cancelled -> completion(
                            Result.failure(
                                if (reason == Constants.CANCELLATION_REASON_SAFETY_LIMIT)
                                    SafetyLimitException()
                                else
                                    java.util.concurrent.CancellationException("Cancelled")
                            )
                        )
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
        topK: Int,
        topP: Float,
        repeatPenalty: Float,
        repeatLastN: Int,
        seed: Int,
        callback: StreamCallback
    )
}

/** Thrown when generation was stopped by a safety limit (time, chars, repetition). */
class SafetyLimitException : Exception("Generation stopped (safety limit)")
