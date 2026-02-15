package com.minicode

data class UiState(
    val instruction: String = "",
    val generatedContent: String = "",
    val isGenerating: Boolean = false,
    val lastError: String? = null
)
