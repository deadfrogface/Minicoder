package com.minicode

data class UiState(
    val projectFolderUri: String? = null,
    val selectedFilePath: String? = null,
    val fileContent: String = "",
    val instruction: String = "",
    val generatedContent: String = "",
    val selectedLanguage: String = "Other",
    val isGenerating: Boolean = false,
    val lastError: String? = null
)
