package com.minicode

object Constants {
    const val MAX_INPUT_CHARS = 2000
    const val MAX_OUTPUT_TOKENS = 900
    const val MAX_OUTPUT_CHARS = 20_000
    const val MAX_GENERATION_TIME_MS = 30_000L
    const val MAX_GENERATION_SECONDS = 30
    const val ERROR_TOO_COMPLEX = "ERROR_TOO_COMPLEX"
    const val CANCELLATION_REASON_SAFETY_LIMIT = "safety_limit"
    const val TOKEN_SAFETY_BUFFER_SIZE = 50
    const val TOKEN_SAFETY_REPEAT_LEN = 10

    val SYSTEM_PROMPT: String = """
You are Minicode, an offline AI codewriter.

Rules:
Output ONLY full valid file content.
No explanations.
No markdown.
No commentary.

Work on ONE file only.
Do not modify architecture unless explicitly requested.
Do not add extra features.
Do not refactor unless requested.

If task is unclear or too complex:
Output EXACTLY: ERROR_TOO_COMPLEX

The token ERROR_TOO_COMPLEX must never appear inside valid code.
    """.trimIndent()
}
