package com.bicy.novel.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    
    private const val APP_DIR = "NovelEditor"
    
    fun getAppDir(): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), APP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    fun getNovelsDir(): File {
        val dir = File(getAppDir(), "novels")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    fun getNovelDir(novelId: Long): File {
        val dir = File(getNovelsDir(), novelId.toString())
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    fun getExportDir(novelId: Long): File {
        val dir = File(getNovelDir(novelId), "export")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    fun getBackupDir(): File {
        val dir = File(getAppDir(), "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    fun exportToTxt(novelTitle: String, chapters: List<Pair<String, String>>, destDir: File): File {
        val fileName = "${novelTitle}_${getTimestamp()}.txt"
        val file = File(destDir, fileName)
        
        val content = StringBuilder().apply {
            append(novelTitle)
            append("\n\n")
            chapters.forEach { (title, content) ->
                append("\n")
                append(title)
                append("\n\n")
                append(content)
                append("\n\n")
            }
        }.toString()
        
        file.writeText(content, Charsets.UTF_8)
        return file
    }
    
    private fun getTimestamp(): String {
        val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return format.format(Date())
    }
    
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> String.format("%.1fKB", size / 1024.0)
            else -> String.format("%.1fMB", size / (1024.0 * 1024.0))
        }
    }
}
