package com.bicy.novel.data.agent

import com.bicy.novel.domain.model.*
import com.bicy.novel.domain.repository.*
import com.bicy.novel.util.WordCounter
import org.json.JSONObject

class EditTool(
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository
) : AgentTool {
    
    override val name = "edit"
    override val description = "编辑已有内容。可以编辑章节、角色、世界观、笔记或时间线事件。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "type" to ToolProperty("string", "要编辑的内容类型：chapter, character, worldview, note, timeline",
                enum = listOf("chapter", "character", "worldview", "note", "timeline")),
            "id" to ToolProperty("number", "要编辑的内容ID"),
            "title" to ToolProperty("string", "新标题（可选）"),
            "content" to ToolProperty("string", "新内容（可选）"),
            "operation" to ToolProperty("string", "章节内容操作方式：replace(替换全部), insert(插入), append(追加), prepend(前置)",
                enum = listOf("replace", "insert", "append", "prepend")),
            "position" to ToolProperty("number", "插入位置（当operation为insert时使用）")
        ),
        required = listOf("type", "id")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val type = params.getString("type")
        val id = params.getLong("id")
        val newTitle = if (params.has("title")) params.getString("title") else null
        val newContent = if (params.has("content")) params.getString("content") else null
        
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
                    
                    val operation = params.optString("operation", "replace")
                    val finalContent = when (operation) {
                        "replace" -> newContent ?: chapter.content
                        "append" -> chapter.content + (newContent ?: "")
                        "prepend" -> (newContent ?: "") + chapter.content
                        "insert" -> {
                            val position = params.optInt("position", chapter.content.length)
                            chapter.content.substring(0, position.coerceIn(0, chapter.content.length)) +
                                (newContent ?: "") +
                                chapter.content.substring(position.coerceIn(0, chapter.content.length))
                        }
                        else -> newContent ?: chapter.content
                    }
                    
                    val updatedChapter = chapter.copy(
                        title = newTitle ?: chapter.title,
                        content = finalContent,
                        wordCount = WordCounter.count(finalContent)
                    )
                    chapterRepository.updateChapter(updatedChapter)
                    
                    val afterData = JSONObject().apply {
                        put("title", updatedChapter.title)
                        put("content", updatedChapter.content)
                        put("wordCount", updatedChapter.wordCount)
                        put("sortOrder", updatedChapter.sortOrder)
                    }.toString()
                    
                    ToolResult.Success(
                        message = "已更新章节「${updatedChapter.title}」",
                        data = mapOf("id" to id, "title" to updatedChapter.title, "wordCount" to updatedChapter.wordCount),
                        operationType = "UPDATE",
                        targetType = "chapter",
                        targetId = id,
                        targetName = updatedChapter.title,
                        beforeData = beforeData,
                        afterData = afterData
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
                    
                    val updatedCharacter = character.copy(
                        name = newTitle ?: character.name,
                        description = newContent ?: character.description
                    )
                    characterRepository.updateCharacter(updatedCharacter)
                    
                    val afterData = JSONObject().apply {
                        put("name", updatedCharacter.name)
                        put("description", updatedCharacter.description)
                    }.toString()
                    
                    ToolResult.Success(
                        message = "已更新角色「${updatedCharacter.name}」",
                        data = mapOf("id" to id, "name" to updatedCharacter.name),
                        operationType = "UPDATE",
                        targetType = "character",
                        targetId = id,
                        targetName = updatedCharacter.name,
                        beforeData = beforeData,
                        afterData = afterData
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
                    
                    val updatedWorldview = worldview.copy(
                        title = newTitle ?: worldview.title,
                        content = newContent ?: worldview.content
                    )
                    worldviewRepository.updateWorldview(updatedWorldview)
                    
                    val afterData = JSONObject().apply {
                        put("title", updatedWorldview.title)
                        put("content", updatedWorldview.content)
                    }.toString()
                    
                    ToolResult.Success(
                        message = "已更新世界观「${updatedWorldview.title}」",
                        data = mapOf("id" to id, "title" to updatedWorldview.title),
                        operationType = "UPDATE",
                        targetType = "worldview",
                        targetId = id,
                        targetName = updatedWorldview.title,
                        beforeData = beforeData,
                        afterData = afterData
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
                    
                    val updatedNote = note.copy(
                        title = newTitle ?: note.title,
                        content = newContent ?: note.content
                    )
                    noteRepository.updateNote(updatedNote)
                    
                    val afterData = JSONObject().apply {
                        put("title", updatedNote.title)
                        put("content", updatedNote.content)
                    }.toString()
                    
                    ToolResult.Success(
                        message = "已更新笔记「${updatedNote.title}」",
                        data = mapOf("id" to id, "title" to updatedNote.title),
                        operationType = "UPDATE",
                        targetType = "note",
                        targetId = id,
                        targetName = updatedNote.title,
                        beforeData = beforeData,
                        afterData = afterData
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
                    
                    val updatedEvent = event.copy(
                        title = newTitle ?: event.title,
                        description = newContent ?: event.description
                    )
                    timelineRepository.updateTimeline(updatedEvent)
                    
                    val afterData = JSONObject().apply {
                        put("title", updatedEvent.title)
                        put("description", updatedEvent.description)
                    }.toString()
                    
                    ToolResult.Success(
                        message = "已更新时间线事件「${updatedEvent.title}」",
                        data = mapOf("id" to id, "title" to updatedEvent.title),
                        operationType = "UPDATE",
                        targetType = "timeline",
                        targetId = id,
                        targetName = updatedEvent.title,
                        beforeData = beforeData,
                        afterData = afterData
                    )
                }
            }
            else -> ToolResult.Error("未知的内容类型：$type")
        }
    }
}
