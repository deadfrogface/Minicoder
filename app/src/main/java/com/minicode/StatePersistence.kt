package com.minicode

import android.content.Context
import android.content.SharedPreferences

class StatePersistence(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var projectFolderUri: String?
        get() = prefs.getString(KEY_PROJECT_URI, null)
        set(value) = prefs.edit().putString(KEY_PROJECT_URI, value).apply()

    var selectedFilePath: String?
        get() = prefs.getString(KEY_SELECTED_FILE, null)
        set(value) = prefs.edit().putString(KEY_SELECTED_FILE, value).apply()

    var instruction: String
        get() = prefs.getString(KEY_INSTRUCTION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_INSTRUCTION, value).apply()

    var lastGeneratedOutput: String
        get() = prefs.getString(KEY_LAST_OUTPUT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_OUTPUT, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "Other") ?: "Other"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun saveState(
        projectUri: String?,
        fileUri: String?,
        instruction: String,
        generatedOutput: String,
        language: String
    ) {
        prefs.edit()
            .putString(KEY_PROJECT_URI, projectUri)
            .putString(KEY_SELECTED_FILE, fileUri)
            .putString(KEY_INSTRUCTION, instruction)
            .putString(KEY_LAST_OUTPUT, generatedOutput)
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun loadState(): UiState {
        return UiState(
            projectFolderUri = projectFolderUri,
            selectedFilePath = selectedFilePath,
            fileContent = "", // must be re-read from file
            instruction = instruction,
            generatedContent = lastGeneratedOutput,
            selectedLanguage = selectedLanguage,
            isGenerating = false,
            lastError = null
        )
    }

    companion object {
        private const val PREFS_NAME = "minicode_state"
        private const val KEY_PROJECT_URI = "project_folder_uri"
        private const val KEY_SELECTED_FILE = "selected_file_uri"
        private const val KEY_INSTRUCTION = "instruction"
        private const val KEY_LAST_OUTPUT = "last_generated_output"
        private const val KEY_LANGUAGE = "language"
    }
}
