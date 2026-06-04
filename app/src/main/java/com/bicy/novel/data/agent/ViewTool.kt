package com.bicy.novel.data.agent

import com.bicy.novel.domain.repository.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class ViewTool(
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository
) : AgentTool {
    
    override val name = "view"
    override val description = "查看指定内容。可以查看章节、角色、世界观、笔记或时间线事件的详细内容。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "type" to ToolProperty("string", "要查看的内容类型：chapter, character, worldview, note, timeline",
                enum = listOf("chapter", "character", "worldview", "note", "timeline")),
            "id" to ToolProperty("number", "要查看的内容ID"),
            "start" to ToolProperty("number", "查看章节时的起始位置（可选）"),
            "end" to ToolProperty("number", "查看章节时的结束位置（可选）")
        ),
        required = listOf("type", "id")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val type = params.getString("type")
        val id = params.getLong("id")
        
        return when (type) {
            "chapter" -> {
                val chapter = chapterRepository.getChapterById(id)
                if (chapter == null) {
                    ToolResult.Error("章节不存在")
                } else {
                    val content = if (params.has("start") && params.has("end")) {
                        val start = params.getInt("start")
                        val end = params.getInt("end")
                        chapter.content.substring(start.coerceIn(0, chapter.content.length), 
                            end.coerceIn(0, chapter.content.length))
                    } else {
                        chapter.content
                    }
                    ToolResult.Success("章节「${chapter.title}」：\n$content",
                        mapOf("title" to chapter.title, "wordCount" to chapter.wordCount))
                }
            }
            "character" -> {
                val character = characterRepository.getCharacterById(id)
                if (character == null) {
                    ToolResult.Error("角色不存在")
                } else {
                    ToolResult.Success("角色「${character.name}」：\n${character.description}",
                        mapOf("name" to character.name))
                }
            }
            "worldview" -> {
                val worldview = worldviewRepository.getWorldviewById(id)
                if (worldview == null) {
                    ToolResult.Error("世界观不存在")
                } else {
                    ToolResult.Success("世界观「${worldview.title}」：\n${worldview.content}",
                        mapOf("title" to worldview.title))
                }
            }
            "note" -> {
                val note = noteRepository.getNoteById(id)
                if (note == null) {
                    ToolResult.Error("笔记不存在")
                } else {
                    ToolResult.Success("笔记「${note.title}」：\n${note.content}",
                        mapOf("title" to note.title))
                }
            }
            "timeline" -> {
                val event = timelineRepository.getTimelineById(id)
                if (event == null) {
                    ToolResult.Error("时间线事件不存在")
                } else {
                    ToolResult.Success("时间线「${event.title}」：\n${event.description}",
                        mapOf("title" to event.title))
                }
            }
            else -> ToolResult.Error("未知的内容类型：$type")
        }
    }
}
