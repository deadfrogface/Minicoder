package com.minicode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.minicode.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fileManager: FileManager
    private lateinit var llmEngine: LlmEngine
    private lateinit var statePersistence: StatePersistence

    private var projectFolderUri: Uri? = null
    private var selectedFileUri: Uri? = null
    private var fileList: List<Pair<String, Uri>> = emptyList()
    private var isGenerating = false

    private val openFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { u ->
            contentResolver.takePersistableUriPermission(
                u,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            projectFolderUri = u
            statePersistence.projectFolderUri = u.toString()
            loadFileList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fileManager = FileManager(this)
        statePersistence = StatePersistence(this)
        llmEngine = LlmEngine(this)

        setupLanguageSpinner()
        loadModelIfNeeded()
        restoreState()
        setupListeners()
    }

    override fun onPause() {
        super.onPause()
        statePersistence.instruction = binding.etInstruction.text.toString()
        statePersistence.selectedLanguage = binding.spinnerLanguage.selectedItem.toString()
        statePersistence.lastGeneratedOutput = binding.tvGeneratedContent.text.toString()
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf("JS", "Python", "HTML", "Other")
        binding.spinnerLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
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
        val state = statePersistence.loadState()
        state.projectFolderUri?.let { uriStr ->
            projectFolderUri = Uri.parse(uriStr)
            loadFileList()
        }
        state.selectedFilePath?.let { uriStr ->
            selectedFileUri = Uri.parse(uriStr)
            loadFileContent()
        }
        binding.etInstruction.setText(state.instruction)
        binding.tvGeneratedContent.text = state.generatedContent
        val langIndex = when (state.selectedLanguage) {
            "JS" -> 0
            "Python" -> 1
            "HTML" -> 2
            else -> 3
        }
        binding.spinnerLanguage.setSelection(langIndex)
    }

    private fun setupListeners() {
        binding.btnSelectFolder.setOnClickListener {
            openFolderLauncher.launch(null)
        }

        binding.listFiles.setOnItemClickListener { _, _, position, _ ->
            if (position < fileList.size) {
                selectedFileUri = fileList[position].second
                statePersistence.selectedFilePath = selectedFileUri?.toString()
                loadFileContent()
            }
        }

        binding.btnGenerate.setOnClickListener {
            if (isGenerating) return
            val uri = selectedFileUri
            if (uri == null) {
                showError("Select a file first")
                return
            }
            val content = fileManager.readFileContent(uri) ?: ""
            if (fileManager.isFileTooLarge(content)) {
                showError(getString(R.string.file_too_large_title), getString(R.string.file_too_large_message))
                return
            }
            val instruction = binding.etInstruction.text.toString()
            val contentForDiff = content
            isGenerating = true
            binding.btnGenerate.isEnabled = false
            binding.btnApply.isEnabled = false
            binding.btnCopy.isEnabled = false

            llmEngine.generate(instruction, content) { result ->
                isGenerating = false
                binding.btnGenerate.isEnabled = true
                binding.btnApply.isEnabled = true
                binding.btnCopy.isEnabled = true
                result.fold(
                    onSuccess = { output ->
                        val (origSpan, genSpan) = DiffUtil.computeLineDiff(contentForDiff, output)
                        binding.tvFileContent.text = origSpan
                        binding.tvGeneratedContent.text = genSpan
                        statePersistence.saveState(
                            projectFolderUri?.toString(),
                            selectedFileUri?.toString(),
                            binding.etInstruction.text.toString(),
                            output,
                            binding.spinnerLanguage.selectedItem.toString()
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

        binding.btnApply.setOnClickListener {
            val uri = selectedFileUri
            if (uri == null) {
                showError("Select a file first")
                return
            }
            val newContent = binding.tvGeneratedContent.text.toString()
            if (newContent.isEmpty()) {
                showError("Nothing to apply. Generate first.")
                return
            }
            val currentContent = fileManager.readFileContent(uri) ?: ""
            val backupUri = fileManager.createBackup(uri, currentContent)
            if (backupUri == null) {
                showError("Could not create backup")
                return
            }
            if (fileManager.applyChanges(uri, newContent)) {
                loadFileContent()
                binding.tvGeneratedContent.text = ""
                statePersistence.lastGeneratedOutput = ""
                Toast.makeText(this, "Changes applied", Toast.LENGTH_SHORT).show()
            } else {
                showError("Failed to apply changes")
            }
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvGeneratedContent.text.toString()
            if (text.isEmpty()) {
                Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
                return
            }
            (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
                ClipData.newPlainText("minicode", text)
            )
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }

        binding.btnRestoreBackup.setOnClickListener {
            val uri = selectedFileUri
            if (uri == null) {
                showError("Select a file first")
                return
            }
            val backups = fileManager.listBackupsForFile(uri)
            if (backups.isEmpty()) {
                showError("No backups found for this file")
                return
            }
            val names = backups.map { it.first }.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.restore_backup))
                .setItems(names) { _, which ->
                    val backupUri = backups[which].second
                    if (fileManager.restoreFromBackup(backupUri, uri)) {
                        loadFileContent()
                        Toast.makeText(this, "Restored", Toast.LENGTH_SHORT).show()
                    } else {
                        showError("Restore failed")
                    }
                }
                .show()
        }
    }

    private fun loadFileList() {
        val uri = projectFolderUri ?: return
        fileList = fileManager.listFilesInFolder(uri)
        binding.listFiles.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            fileList.map { it.first }
        )
    }

    private fun loadFileContent() {
        val uri = selectedFileUri ?: return
        binding.tvFileContent.text = fileManager.readFileContent(uri) ?: ""
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, null).show()
    }
}
