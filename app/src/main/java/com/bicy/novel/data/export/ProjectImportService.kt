package com.bicy.novel.data.export

import android.content.Context
import android.net.Uri
import com.bicy.novel.domain.model.*
import com.bicy.novel.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectImportService @Inject constructor(
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

    data class ImportMeta(
        val version: Int = 1,
        val encrypted: Boolean = false,
        val title: String = "",
        val exportTime: Long = 0
    )

    suspend fun checkImportFile(inputUri: Uri): ImportMeta = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "meta.txt") {
                            val content = zipIn.readBytes().toString(Charsets.UTF_8)
                            return@withContext parseMeta(content)
                        }
                        entry = zipIn.nextEntry
                    }
                }
            }
            ImportMeta()
        } catch (e: Exception) {
            ImportMeta()
        }
    }

    suspend fun importProject(
        inputUri: Uri,
        password: String? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val meta = checkImportFile(inputUri)
            
            if (meta.encrypted && password.isNullOrEmpty()) {
                return@withContext ImportResult.NeedPassword()
            }

            var jsonContent: String? = null

            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "project.json" || entry.name == "project_encrypted.json") {
                            val bytes = zipIn.readBytes()
                            jsonContent = if (meta.encrypted && !password.isNullOrEmpty()) {
                                try {
                                    decryptData(bytes, password)
                                } catch (e: Exception) {
                                    return@withContext ImportResult.Error("密码错误或文件已损坏")
                                }
                            } else {
                                bytes.toString(Charsets.UTF_8)
                            }
                            break
                        }
                        entry = zipIn.nextEntry
                    }
                }
            }

            if (jsonContent == null) {
                return@withContext ImportResult.Error("无效的项目文件：缺少项目数据")
            }

            val exportData = try {
                json.decodeFromString<NovelExportData>(jsonContent!!)
            } catch (e: Exception) {
                return@withContext ImportResult.Error("项目文件格式错误：${e.message}")
            }

            if (exportData.novel.title.isBlank()) {
                return@withContext ImportResult.Error("项目标题不能为空")
            }

            val novelId = createNovelFromImport(exportData)

            ImportResult.Success(novelId)
        } catch (e: Exception) {
            ImportResult.Error("导入失败: ${e.message}")
        }
    }

    private suspend fun createNovelFromImport(exportData: NovelExportData): Long {
        val novel = Novel(
            title = exportData.novel.title,
            author = exportData.novel.author,
            description = exportData.novel.description,
            coverPath = exportData.novel.coverPath,
            category = exportData.novel.category,
            status = exportData.novel.status,
            totalWords = exportData.novel.totalWords,
            chapterCount = exportData.novel.chapterCount,
            lastEditedType = exportData.novel.lastEditedType,
            createdAt = exportData.novel.createdAt,
            updatedAt = exportData.novel.updatedAt
        )

        val novelId = novelRepository.createNovel(novel)
        
        // ID映射：旧ID -> 新ID
        val chapterIdMap = mutableMapOf<Long, Long>()
        val characterIdMap = mutableMapOf<Long, Long>()
        val worldviewIdMap = mutableMapOf<Long, Long>()
        val noteIdMap = mutableMapOf<Long, Long>()
        val timelineIdMap = mutableMapOf<Long, Long>()

        exportData.chapters.sortedBy { it.sortOrder }.forEachIndexed { index, chapterData ->
            // 假设旧ID是按顺序的，从1开始
            val oldId = index + 1L
            val newId = chapterRepository.createChapter(Chapter(
                novelId = novelId,
                title = chapterData.title,
                content = chapterData.content,
                wordCount = chapterData.wordCount,
                sortOrder = chapterData.sortOrder,
                createdAt = chapterData.createdAt,
                updatedAt = chapterData.updatedAt
            ))
            chapterIdMap[oldId] = newId
        }

        exportData.characters.forEachIndexed { index, charData ->
            val oldId = index + 1L
            val newId = characterRepository.createCharacter(Character(
                novelId = novelId,
                name = charData.name,
                alias = charData.alias,
                roleType = charData.roleType,
                description = charData.description,
                avatarPath = charData.avatarPath,
                attributes = charData.attributes,
                createdAt = charData.createdAt,
                updatedAt = charData.updatedAt
            ))
            characterIdMap[oldId] = newId
        }

        exportData.worldviews.forEachIndexed { index, worldData ->
            val oldId = index + 1L
            val newId = worldviewRepository.createWorldview(Worldview(
                novelId = novelId,
                title = worldData.title,
                category = worldData.category,
                content = worldData.content,
                createdAt = worldData.createdAt,
                updatedAt = worldData.updatedAt
            ))
            worldviewIdMap[oldId] = newId
        }

        exportData.notes.forEachIndexed { index, noteData ->
            val oldId = index + 1L
            val newId = noteRepository.createNote(Note(
                novelId = novelId,
                title = noteData.title,
                content = noteData.content,
                category = noteData.category,
                createdAt = noteData.createdAt,
                updatedAt = noteData.updatedAt
            ))
            noteIdMap[oldId] = newId
        }

        exportData.timelineEvents.sortedBy { it.sortOrder }.forEachIndexed { index, eventData ->
            val oldId = index + 1L
            val newId = timelineRepository.createTimeline(TimelineEvent(
                novelId = novelId,
                title = eventData.title,
                description = eventData.description,
                eventDate = eventData.eventDate,
                sortOrder = eventData.sortOrder,
                isKeyEvent = eventData.isKeyEvent,
                createdAt = eventData.createdAt,
                updatedAt = eventData.updatedAt
            ))
            timelineIdMap[oldId] = newId
        }
        
        // 导入历史记录
        exportData.contentHistories.forEach { historyData ->
            val newTargetId = when (historyData.targetType) {
                "chapter" -> chapterIdMap[historyData.targetId]
                "character" -> characterIdMap[historyData.targetId]
                "worldview" -> worldviewIdMap[historyData.targetId]
                "note" -> noteIdMap[historyData.targetId]
                "timeline" -> timelineIdMap[historyData.targetId]
                else -> null
            }
            
            if (newTargetId != null) {
                try {
                    contentHistoryRepository.saveHistory(
                        targetType = historyData.targetType,
                        targetId = newTargetId,
                        title = historyData.title,
                        content = historyData.content,
                        wordCount = historyData.wordCount,
                        note = historyData.note
                    )
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
        }

        return novelId
    }

    private fun parseMeta(content: String): ImportMeta {
        val lines = content.lines()
        var version = 1
        var encrypted = false
        var title = ""
        var exportTime = 0L

        lines.forEach { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) {
                when (parts[0]) {
                    "version" -> version = parts[1].toIntOrNull() ?: 1
                    "encrypted" -> encrypted = parts[1].toBooleanStrictOrNull() ?: false
                    "title" -> title = parts[1]
                    "exportTime" -> exportTime = parts[1].toLongOrNull() ?: 0L
                }
            }
        }

        return ImportMeta(version, encrypted, title, exportTime)
    }

    private fun decryptData(data: ByteArray, password: String): String {
        val key = password.padEnd(16, '0').take(16).toByteArray(Charsets.UTF_8)
        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decrypted = cipher.doFinal(data)
        return decrypted.toString(Charsets.UTF_8)
    }
}
