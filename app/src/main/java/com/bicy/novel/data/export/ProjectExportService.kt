package com.bicy.novel.data.export

import android.content.Context
import android.net.Uri
import com.bicy.novel.domain.model.*
import com.bicy.novel.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(val novelId: Long) : ImportResult()
    data class NeedPassword(val message: String = "项目已加密，请输入密码") : ImportResult()
    data class Error(val message: String) : ImportResult()
}

@Singleton
class ProjectExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository,
    private val contentHistoryRepository: ContentHistoryRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportProject(
        novelId: Long,
        outputUri: Uri,
        password: String? = null,
        includeChapters: Boolean = true,
        includeCharacters: Boolean = true,
        includeWorldviews: Boolean = true,
        includeNotes: Boolean = true,
        includeTimeline: Boolean = true
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val novel = novelRepository.getNovelById(novelId)
                ?: return@withContext ExportResult.Error("项目不存在")

            val chapters = if (includeChapters) {
                chapterRepository.getChaptersByNovelIdOnce(novelId)
            } else emptyList()

            val characters = if (includeCharacters) {
                try {
                    characterRepository.getCharactersByNovelId(novelId).first()
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()

            val worldviews = if (includeWorldviews) {
                try {
                    worldviewRepository.getWorldviewsByNovelId(novelId).first()
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()

            val notes = if (includeNotes) {
                try {
                    noteRepository.getNotesByNovelId(novelId).first()
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()

            val timelineEvents = if (includeTimeline) {
                try {
                    timelineRepository.getTimelineByNovelId(novelId).first()
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
            
            // 导出历史记录
            val contentHistories = mutableListOf<ContentHistory>()
            if (includeChapters) {
                chapters.forEach { chapter ->
                    try {
                        val histories = contentHistoryRepository.getRecentHistory(chapter.id, "chapter", 50)
                        contentHistories.addAll(histories)
                    } catch (e: Exception) {
                        // 忽略错误
                    }
                }
            }
            if (includeCharacters) {
                characters.forEach { character ->
                    try {
                        val histories = contentHistoryRepository.getRecentHistory(character.id, "character", 50)
                        contentHistories.addAll(histories)
                    } catch (e: Exception) {
                        // 忽略错误
                    }
                }
            }
            if (includeWorldviews) {
                worldviews.forEach { worldview ->
                    try {
                        val histories = contentHistoryRepository.getRecentHistory(worldview.id, "worldview", 50)
                        contentHistories.addAll(histories)
                    } catch (e: Exception) {
                        // 忽略错误
                    }
                }
            }
            if (includeNotes) {
                notes.forEach { note ->
                    try {
                        val histories = contentHistoryRepository.getRecentHistory(note.id, "note", 50)
                        contentHistories.addAll(histories)
                    } catch (e: Exception) {
                        // 忽略错误
                    }
                }
            }
            if (includeTimeline) {
                timelineEvents.forEach { event ->
                    try {
                        val histories = contentHistoryRepository.getRecentHistory(event.id, "timeline", 50)
                        contentHistories.addAll(histories)
                    } catch (e: Exception) {
                        // 忽略错误
                    }
                }
            }

            val exportData = NovelExportData(
                novel = novel.toExportData(),
                chapters = chapters.map { it.toExportData() },
                characters = characters.map { it.toExportData() },
                worldviews = worldviews.map { it.toExportData() },
                notes = notes.map { it.toExportData() },
                timelineEvents = timelineEvents.map { it.toExportData() },
                contentHistories = contentHistories.map { it.toExportData() }
            )

            val jsonContent = json.encodeToString(exportData)
            val contentToWrite = if (!password.isNullOrEmpty()) {
                encryptData(jsonContent, password)
            } else {
                jsonContent.toByteArray(Charsets.UTF_8)
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                BufferedOutputStream(outputStream).use { bufferedStream ->
                    ZipOutputStream(bufferedStream).use { zipOut ->
                        val entryName = if (!password.isNullOrEmpty()) {
                            "project_encrypted.json"
                        } else {
                            "project.json"
                        }
                        val entry = ZipEntry(entryName)
                        zipOut.putNextEntry(entry)
                        zipOut.write(contentToWrite)
                        zipOut.closeEntry()

                        val metaEntry = ZipEntry("meta.txt")
                        zipOut.putNextEntry(metaEntry)
                        val metaContent = buildString {
                            appendLine("version=1")
                            appendLine("encrypted=${!password.isNullOrEmpty()}")
                            appendLine("title=${novel.title}")
                            appendLine("exportTime=${System.currentTimeMillis()}")
                        }
                        zipOut.write(metaContent.toByteArray(Charsets.UTF_8))
                        zipOut.closeEntry()
                    }
                }
            } ?: return@withContext ExportResult.Error("无法打开输出文件")

            ExportResult.Success(outputUri)
        } catch (e: Exception) {
            ExportResult.Error("导出失败: ${e.message}")
        }
    }

    private fun encryptData(data: String, password: String): ByteArray {
        val key = password.padEnd(16, '0').take(16).toByteArray(Charsets.UTF_8)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    }
}
