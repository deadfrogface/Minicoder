package com.minicode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.minicode.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var llmEngine: LlmEngine
    private lateinit var statePersistence: StatePersistence

    private var isGenerating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statePersistence = StatePersistence(this)
        llmEngine = LlmEngine(this)

        loadModelIfNeeded()
        restoreState()
        setupListeners()
    }

    override fun onPause() {
        super.onPause()
        statePersistence.instruction = binding.etInstruction.text.toString()
        statePersistence.lastGeneratedOutput = binding.tvGeneratedContent.text.toString()
    }

    private fun loadModelIfNeeded() {
        val modelName = "deepseek-coder-1.3b-q4.gguf"
        val destFile = java.io.File(filesDir, modelName)
        if (!destFile.exists()) {
            try {
                assets.open(modelName).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // Model not in assets; user must add it
            }
        }
        if (destFile.exists()) {
            llmEngine.loadModel(destFile.absolutePath)
        }
    }

    private fun restoreState() {
        binding.etInstruction.setText(statePersistence.instruction)
        binding.tvGeneratedContent.text = statePersistence.lastGeneratedOutput
    }

    private fun setupListeners() {
        binding.btnGenerate.setOnClickListener {
            if (isGenerating) return@setOnClickListener
            val instruction = binding.etInstruction.text.toString()
            if (instruction.isBlank()) {
                showError(getString(R.string.enter_instruction))
                return@setOnClickListener
            }
            isGenerating = true
            binding.btnGenerate.isEnabled = false
            binding.btnCopy.isEnabled = false

            llmEngine.generate(instruction, "") { result ->
                isGenerating = false
                binding.btnGenerate.isEnabled = true
                binding.btnCopy.isEnabled = true
                result.fold(
                    onSuccess = { output ->
                        binding.tvGeneratedContent.text = output
                        statePersistence.saveState(
                            binding.etInstruction.text.toString(),
                            output
                        )
                    },
                    onFailure = { e ->
                        val msg = if (e.message == Constants.ERROR_TOO_COMPLEX) {
                            getString(R.string.error_too_complex)
                        } else e.message ?: "Generation failed"
                        showError(msg)
                    }
                )
            }
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvGeneratedContent.text.toString()
            if (text.isEmpty()) {
                Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
                ClipData.newPlainText("minicode", text)
            )
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
