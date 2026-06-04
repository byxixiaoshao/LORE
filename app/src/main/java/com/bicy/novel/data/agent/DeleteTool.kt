package com.bicy.novel.data.agent

import com.bicy.novel.domain.repository.*
import org.json.JSONObject

class DeleteTool(
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository
) : AgentTool {
    
    override val name = "delete"
    override val description = "删除指定内容。可以删除章节、角色、世界观、笔记或时间线事件。注意：删除操作不可撤销。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "type" to ToolProperty("string", "要删除的内容类型：chapter, character, worldview, note, timeline",
                enum = listOf("chapter", "character", "worldview", "note", "timeline")),
            "id" to ToolProperty("number", "要删除的内容ID")
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
                    val beforeData = JSONObject().apply {
                        put("title", chapter.title)
                        put("content", chapter.content)
                        put("wordCount", chapter.wordCount)
                        put("sortOrder", chapter.sortOrder)
                    }.toString()
                    
                    chapterRepository.deleteChapter(id)
                    ToolResult.Success(
                        message = "已删除章节「${chapter.title}」",
                        data = mapOf("id" to id),
                        operationType = "DELETE",
                        targetType = "chapter",
                        targetId = id,
                        targetName = chapter.title,
                        beforeData = beforeData,
                        afterData = null
                    )
                }
            }
            "character" -> {
                val character = characterRepository.getCharacterById(id)
                if (character == null) {
                    ToolResult.Error("角色不存在")
                } else {
                    val beforeData = JSONObject().apply {
                        put("name", character.name)
                        put("description", character.description)
                    }.toString()
                    
                    characterRepository.deleteCharacter(character)
                    ToolResult.Success(
                        message = "已删除角色「${character.name}」",
                        data = mapOf("id" to id),
                        operationType = "DELETE",
                        targetType = "character",
                        targetId = id,
                        targetName = character.name,
                        beforeData = beforeData,
                        afterData = null
                    )
                }
            }
            "worldview" -> {
                val worldview = worldviewRepository.getWorldviewById(id)
                if (worldview == null) {
                    ToolResult.Error("世界观不存在")
                } else {
                    val beforeData = JSONObject().apply {
                        put("title", worldview.title)
                        put("content", worldview.content)
                    }.toString()
                    
                    worldviewRepository.deleteWorldview(worldview)
                    ToolResult.Success(
                        message = "已删除世界观「${worldview.title}」",
                        data = mapOf("id" to id),
                        operationType = "DELETE",
                        targetType = "worldview",
                        targetId = id,
                        targetName = worldview.title,
                        beforeData = beforeData,
                        afterData = null
                    )
                }
            }
            "note" -> {
                val note = noteRepository.getNoteById(id)
                if (note == null) {
                    ToolResult.Error("笔记不存在")
                } else {
                    val beforeData = JSONObject().apply {
                        put("title", note.title)
                        put("content", note.content)
                    }.toString()
                    
                    noteRepository.deleteNote(note)
                    ToolResult.Success(
                        message = "已删除笔记「${note.title}」",
                        data = mapOf("id" to id),
                        operationType = "DELETE",
                        targetType = "note",
                        targetId = id,
                        targetName = note.title,
                        beforeData = beforeData,
                        afterData = null
                    )
                }
            }
            "timeline" -> {
                val event = timelineRepository.getTimelineById(id)
                if (event == null) {
                    ToolResult.Error("时间线事件不存在")
                } else {
                    val beforeData = JSONObject().apply {
                        put("title", event.title)
                        put("description", event.description)
                    }.toString()
                    
                    timelineRepository.deleteTimeline(event)
                    ToolResult.Success(
                        message = "已删除时间线事件「${event.title}」",
                        data = mapOf("id" to id),
                        operationType = "DELETE",
                        targetType = "timeline",
                        targetId = id,
                        targetName = event.title,
                        beforeData = beforeData,
                        afterData = null
                    )
                }
            }
            else -> ToolResult.Error("未知的内容类型：$type")
        }
    }
}
