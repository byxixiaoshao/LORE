package com.bicy.novel.data.export

import android.content.Context
import android.net.Uri
import com.bicy.novel.domain.model.*
import com.bicy.novel.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TxtExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository
) {
    suspend fun exportAsTxt(
        novelId: Long,
        outputUri: Uri,
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

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                BufferedOutputStream(outputStream).use { bufferedStream ->
                    ZipOutputStream(bufferedStream).use { zipOut ->
                        // 小说信息
                        val novelInfo = buildString {
                            appendLine("【小说信息】")
                            appendLine("标题：${novel.title}")
                            appendLine("作者：${novel.author}")
                            appendLine("分类：${novel.category}")
                            appendLine("状态：${getStatusText(novel.status)}")
                            appendLine("总字数：${novel.totalWords}")
                            appendLine("章节数：${novel.chapterCount}")
                            appendLine("创建时间：${formatTime(novel.createdAt)}")
                            appendLine("更新时间：${formatTime(novel.updatedAt)}")
                            appendLine()
                            appendLine("【简介】")
                            appendLine(novel.description.ifEmpty { "暂无简介" })
                        }
                        writeZipEntry(zipOut, "${sanitizeFileName(novel.title)}/小说信息.txt", novelInfo)

                        // 章节内容
                        if (chapters.isNotEmpty()) {
                            chapters.sortedBy { it.sortOrder }.forEachIndexed { index, chapter ->
                                val chapterContent = buildString {
                                    appendLine("【${chapter.title}】")
                                    appendLine()
                                    appendLine(chapter.content)
                                    appendLine()
                                    appendLine("——")
                                    appendLine("字数：${chapter.wordCount}")
                                    appendLine("创建时间：${formatTime(chapter.createdAt)}")
                                    appendLine("更新时间：${formatTime(chapter.updatedAt)}")
                                }
                                val fileName = "${index + 1}_${sanitizeFileName(chapter.title)}.txt"
                                writeZipEntry(
                                    zipOut,
                                    "${sanitizeFileName(novel.title)}/章节/$fileName",
                                    chapterContent
                                )
                            }
                        }

                        // 角色设定
                        if (characters.isNotEmpty()) {
                            characters.forEach { character ->
                                val characterContent = buildString {
                                    appendLine("【${character.name}】")
                                    if (character.alias.isNotEmpty()) {
                                        appendLine("别名：${character.alias}")
                                    }
                                    appendLine("类型：${character.roleType}")
                                    appendLine()
                                    appendLine("【描述】")
                                    appendLine(character.description.ifEmpty { "暂无描述" })
                                    appendLine()
                                    if (character.attributes.isNotEmpty()) {
                                        appendLine("【属性】")
                                        appendLine(character.attributes)
                                    }
                                }
                                writeZipEntry(
                                    zipOut,
                                    "${sanitizeFileName(novel.title)}/角色/${sanitizeFileName(character.name)}.txt",
                                    characterContent
                                )
                            }
                        }

                        // 世界观设定
                        if (worldviews.isNotEmpty()) {
                            worldviews.forEach { worldview ->
                                val worldviewContent = buildString {
                                    appendLine("【${worldview.title}】")
                                    appendLine("分类：${worldview.category}")
                                    appendLine()
                                    appendLine(worldview.content)
                                }
                                writeZipEntry(
                                    zipOut,
                                    "${sanitizeFileName(novel.title)}/世界观/${sanitizeFileName(worldview.title)}.txt",
                                    worldviewContent
                                )
                            }
                        }

                        // 大纲笔记
                        if (notes.isNotEmpty()) {
                            notes.forEach { note ->
                                val noteContent = buildString {
                                    appendLine("【${note.title}】")
                                    appendLine("分类：${note.category}")
                                    appendLine()
                                    appendLine(note.content)
                                }
                                writeZipEntry(
                                    zipOut,
                                    "${sanitizeFileName(novel.title)}/笔记/${sanitizeFileName(note.title)}.txt",
                                    noteContent
                                )
                            }
                        }

                        // 时间线
                        if (timelineEvents.isNotEmpty()) {
                            val timelineContent = buildString {
                                appendLine("【时间线】")
                                appendLine()
                                timelineEvents.sortedBy { it.sortOrder }.forEach { event ->
                                    appendLine("【${event.title}】")
                                    appendLine("时间：${event.eventDate}")
                                    if (event.isKeyEvent) {
                                        appendLine("★ 关键事件")
                                    }
                                    appendLine(event.description)
                                    appendLine()
                                }
                            }
                            writeZipEntry(
                                zipOut,
                                "${sanitizeFileName(novel.title)}/时间线.txt",
                                timelineContent
                            )
                        }
                    }
                }
            } ?: return@withContext ExportResult.Error("无法打开输出文件")

            ExportResult.Success(outputUri)
        } catch (e: Exception) {
            ExportResult.Error("导出失败: ${e.message}")
        }
    }

    private fun writeZipEntry(zipOut: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zipOut.putNextEntry(entry)
        zipOut.write(content.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    private fun getStatusText(status: Int): String {
        return when (status) {
            0 -> "连载中"
            1 -> "已完结"
            2 -> "暂停"
            else -> "未知"
        }
    }

    private fun formatTime(timestamp: Long): String {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }
}
