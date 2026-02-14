package com.minicode

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileManager(private val context: Context) {

    private val backupsDir: File
        get() = File(context.filesDir, "backups").also { if (!it.exists()) it.mkdirs() }

    fun listFilesInFolder(treeUri: Uri): List<Pair<String, Uri>> {
        val doc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        if (!doc.isDirectory) return emptyList()
        return doc.listFiles()
            .filter { it.isFile }
            .map { it.name!! to it.uri }
    }

    fun readFileContent(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            null
        }
    }

    fun isFileTooLarge(content: String): Boolean =
        content.length > 15_000 || content.count { it == '\n' } >= 600

    fun getFileName(uri: Uri): String? = DocumentFile.fromSingleUri(context, uri)?.name

    fun createBackup(originalUri: Uri, content: String): Uri? {
        val name = getFileName(originalUri) ?: "file"
        val ext = name.substringAfterLast('.', "")
        val base = name.substringBeforeLast('.').ifEmpty { name }
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val backupName = "${base}_backup_${timestamp}.${if (ext.isEmpty()) "bak" else ext}"
        val backupFile = File(backupsDir, backupName)
        return try {
            backupFile.writeText(content)
            Uri.fromFile(backupFile)
        } catch (e: Exception) {
            null
        }
    }

    fun applyChanges(uri: Uri, newContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out: OutputStream ->
                out.write(newContent.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun listBackupsForFile(originalUri: Uri): List<Pair<String, Uri>> {
        val name = getFileName(originalUri) ?: return emptyList()
        val base = name.substringBeforeLast('.').ifEmpty { name }
        return backupsDir.listFiles()?.orEmpty()
            ?.filter { it.name.startsWith("${base}_backup_") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.name to Uri.fromFile(it) }
            ?: emptyList()
    }

    fun restoreFromBackup(backupUri: Uri, targetUri: Uri): Boolean {
        val content = readFileContent(backupUri) ?: return false
        return applyChanges(targetUri, content)
    }
}
