package com.minicode

import android.content.Context
import android.content.SharedPreferences

class StatePersistence(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var instruction: String
        get() = prefs.getString(KEY_INSTRUCTION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_INSTRUCTION, value).apply()

    var lastGeneratedOutput: String
        get() = prefs.getString(KEY_LAST_OUTPUT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_OUTPUT, value).apply()

    var useCamelCase: Boolean
        get() = prefs.getBoolean(KEY_USE_CAMELCASE, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_CAMELCASE, value).apply()

    fun saveState(instruction: String, generatedOutput: String, useCamelCase: Boolean = false) {
        prefs.edit()
            .putString(KEY_INSTRUCTION, instruction)
            .putString(KEY_LAST_OUTPUT, generatedOutput)
            .putBoolean(KEY_USE_CAMELCASE, useCamelCase)
            .apply()
    }

    fun loadState(): UiState {
        return UiState(
            instruction = instruction,
            generatedContent = lastGeneratedOutput,
            isGenerating = false,
            lastError = null
        )
    }

    companion object {
        private const val PREFS_NAME = "minicode_state"
        private const val KEY_INSTRUCTION = "instruction"
        private const val KEY_LAST_OUTPUT = "last_generated_output"
        private const val KEY_USE_CAMELCASE = "use_camelcase"
    }
}
