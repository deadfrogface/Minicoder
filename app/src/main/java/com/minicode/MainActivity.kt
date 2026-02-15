package com.minicode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.minicode.databinding.ActivityMainBinding
import java.util.concurrent.CancellationException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var llmEngine: LlmEngine
    private lateinit var statePersistence: StatePersistence

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
        statePersistence.useCamelCase = binding.switchCamelCase.isChecked
        // Output is saved only on successful generation completion, not here (no partial output).
    }

    override fun onDestroy() {
        if (llmEngine.isGenerating()) {
            llmEngine.cancelGeneration()
        }
        llmEngine.unloadModel()
        super.onDestroy()
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
                // Model not in assets
            }
        }
        if (destFile.exists()) {
            llmEngine.loadModel(destFile.absolutePath)
        }
    }

    private fun restoreState() {
        binding.etInstruction.setText(statePersistence.instruction)
        binding.tvGeneratedContent.text = statePersistence.lastGeneratedOutput
        binding.switchCamelCase.isChecked = statePersistence.useCamelCase
        showGenerateButton()
        binding.btnCopy.isEnabled = binding.tvGeneratedContent.text.isNotEmpty()
    }

    private fun setupListeners() {
        binding.btnGenerate.setOnClickListener {
            if (llmEngine.isGenerating()) return@setOnClickListener
            val instruction = binding.etInstruction.text.toString().trim()
            if (instruction.isBlank()) {
                showError(getString(R.string.enter_instruction))
                return@setOnClickListener
            }
            binding.tvGeneratedContent.text = getString(R.string.generating)
            binding.btnCopy.isEnabled = false
            showCancelButton()

            llmEngine.generateStream(
                instruction = instruction,
                fileContent = "",
                useCamelCase = binding.switchCamelCase.isChecked,
                onToken = { token ->
                    val current = binding.tvGeneratedContent.text.toString()
                    if (current == getString(R.string.generating)) {
                        binding.tvGeneratedContent.text = token
                    } else {
                        binding.tvGeneratedContent.append(token)
                    }
                },
                completion = { result ->
                    showGenerateButton()
                    result.fold(
                        onSuccess = { output ->
                            binding.tvGeneratedContent.text = output
                            binding.btnCopy.isEnabled = output.isNotEmpty()
                            statePersistence.saveState(
                                binding.etInstruction.text.toString(),
                                output,
                                binding.switchCamelCase.isChecked
                            )
                        },
                        onFailure = { e ->
                            when (e) {
                                is SafetyLimitException ->
                                    Toast.makeText(
                                        this,
                                        getString(R.string.generation_stopped_safety_limit),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                is CancellationException -> { /* user cancelled */ }
                                else -> {
                                    val msg = when (e.message) {
                                        Constants.ERROR_TOO_COMPLEX -> getString(R.string.error_too_complex)
                                        else -> e.message ?: "Generation failed"
                                    }
                                    showError(msg)
                                }
                            }
                            if (e is CancellationException)
                                binding.tvGeneratedContent.text = statePersistence.lastGeneratedOutput
                            binding.btnCopy.isEnabled = binding.tvGeneratedContent.text.isNotEmpty()
                        }
                    )
                }
            )
        }

        binding.btnCancel.setOnClickListener {
            llmEngine.cancelGeneration()
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvGeneratedContent.text.toString()
            if (text.isEmpty()) return@setOnClickListener
            (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
                ClipData.newPlainText("minicode", text)
            )
            Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }

        binding.switchCamelCase.setOnCheckedChangeListener { _, _ ->
            statePersistence.useCamelCase = binding.switchCamelCase.isChecked
        }
    }

    private fun showGenerateButton() {
        binding.btnGenerate.visibility = View.VISIBLE
        binding.btnCancel.visibility = View.GONE
        binding.btnGenerate.isEnabled = true
    }

    private fun showCancelButton() {
        binding.btnGenerate.visibility = View.GONE
        binding.btnCancel.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
