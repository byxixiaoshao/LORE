package com.bicy.novel.data.agent

import com.bicy.novel.domain.model.*
import com.bicy.novel.domain.repository.*
import org.json.JSONObject

class CreateTool(
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository
) : AgentTool {
    
    override val name = "create"
    override val description = "创建新内容。可以创建章节、角色、世界观、笔记或时间线事件。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "type" to ToolProperty("string", "要创建的内容类型：chapter, character, worldview, note, timeline",
                enum = listOf("chapter", "character", "worldview", "note", "timeline")),
            "title" to ToolProperty("string", "标题或名称"),
            "content" to ToolProperty("string", "内容描述（可选）"),
            "sort_order" to ToolProperty("number", "章节排序顺序（可选，仅用于章节）")
        ),
        required = listOf("type", "title")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val type = params.getString("type")
        val title = params.getString("title")
        val content = params.optString("content", "")
        
        return when (type) {
            "chapter" -> {
                val sortOrder = params.optInt("sort_order", 
                    chapterRepository.getChapterCount(context.novelId) + 1)
                val chapter = Chapter(
                    novelId = context.novelId,
                    title = title,
                    content = content,
                    wordCount = content.length,
                    sortOrder = sortOrder
                )
                val id = chapterRepository.createChapter(chapter)
                val afterData = JSONObject().apply {
                    put("title", title)
                    put("content", content)
                    put("wordCount", content.length)
                    put("sortOrder", sortOrder)
                }.toString()
                ToolResult.Success(
                    message = "已创建章节「$title」，ID: $id",
                    data = mapOf("id" to id, "title" to title),
                    operationType = "CREATE",
                    targetType = "chapter",
                    targetId = id,
                    targetName = title,
                    beforeData = null,
                    afterData = afterData
                )
            }
            "character" -> {
                val character = Character(
                    novelId = context.novelId,
                    name = title,
                    description = content
                )
                val id = characterRepository.createCharacter(character)
                val afterData = JSONObject().apply {
                    put("name", title)
                    put("description", content)
                }.toString()
                ToolResult.Success(
                    message = "已创建角色「$title」，ID: $id",
                    data = mapOf("id" to id, "name" to title),
                    operationType = "CREATE",
                    targetType = "character",
                    targetId = id,
                    targetName = title,
                    beforeData = null,
                    afterData = afterData
                )
            }
            "worldview" -> {
                val worldview = Worldview(
                    novelId = context.novelId,
                    title = title,
                    content = content
                )
                val id = worldviewRepository.createWorldview(worldview)
                val afterData = JSONObject().apply {
                    put("title", title)
                    put("content", content)
                }.toString()
                ToolResult.Success(
                    message = "已创建世界观「$title」，ID: $id",
                    data = mapOf("id" to id, "title" to title),
                    operationType = "CREATE",
                    targetType = "worldview",
                    targetId = id,
                    targetName = title,
                    beforeData = null,
                    afterData = afterData
                )
            }
            "note" -> {
                val note = Note(
                    novelId = context.novelId,
                    title = title,
                    content = content
                )
                val id = noteRepository.createNote(note)
                val afterData = JSONObject().apply {
                    put("title", title)
                    put("content", content)
                }.toString()
                ToolResult.Success(
                    message = "已创建笔记「$title」，ID: $id",
                    data = mapOf("id" to id, "title" to title),
                    operationType = "CREATE",
                    targetType = "note",
                    targetId = id,
                    targetName = title,
                    beforeData = null,
                    afterData = afterData
                )
            }
            "timeline" -> {
                val event = TimelineEvent(
                    novelId = context.novelId,
                    title = title,
                    description = content
                )
                val id = timelineRepository.createTimeline(event)
                val afterData = JSONObject().apply {
                    put("title", title)
                    put("description", content)
                }.toString()
                ToolResult.Success(
                    message = "已创建时间线事件「$title」，ID: $id",
                    data = mapOf("id" to id, "title" to title),
                    operationType = "CREATE",
                    targetType = "timeline",
                    targetId = id,
                    targetName = title,
                    beforeData = null,
                    afterData = afterData
                )
            }
            else -> ToolResult.Error("未知的内容类型：$type")
        }
    }
}
