package com.bicy.novel.data.agent

import com.bicy.novel.domain.repository.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class ListCharactersTool(
    private val characterRepository: CharacterRepository
) : AgentTool {
    
    override val name = "list_characters"
    override val description = "列出当前小说的所有角色。"
    override val parameters = ToolParameters(
        properties = emptyMap(),
        required = emptyList()
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val characters = characterRepository.getCharactersByNovelId(context.novelId).first()
        
        if (characters.isEmpty()) {
            return ToolResult.Success("当前小说暂无角色")
        }
        
        val list = characters.mapIndexed { index, char ->
            "${index + 1}. ${char.name} (ID: ${char.id}) - ${char.description.take(50)}..."
        }.joinToString("\n")
        
        return ToolResult.Success("角色列表：\n$list",
            mapOf("count" to characters.size, "ids" to characters.map { it.id }))
    }
}

class ListWorldviewsTool(
    private val worldviewRepository: WorldviewRepository
) : AgentTool {
    
    override val name = "list_worldviews"
    override val description = "列出当前小说的所有世界观设定。"
    override val parameters = ToolParameters(
        properties = emptyMap(),
        required = emptyList()
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val worldviews = worldviewRepository.getWorldviewsByNovelId(context.novelId).first()
        
        if (worldviews.isEmpty()) {
            return ToolResult.Success("当前小说暂无世界观设定")
        }
        
        val list = worldviews.mapIndexed { index, wv ->
            "${index + 1}. ${wv.title} (ID: ${wv.id}) - ${wv.content.take(50)}..."
        }.joinToString("\n")
        
        return ToolResult.Success("世界观列表：\n$list",
            mapOf("count" to worldviews.size, "ids" to worldviews.map { it.id }))
    }
}

class ListNotesTool(
    private val noteRepository: NoteRepository
) : AgentTool {
    
    override val name = "list_notes"
    override val description = "列出当前小说的所有笔记。"
    override val parameters = ToolParameters(
        properties = emptyMap(),
        required = emptyList()
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val notes = noteRepository.getNotesByNovelId(context.novelId).first()
        
        if (notes.isEmpty()) {
            return ToolResult.Success("当前小说暂无笔记")
        }
        
        val list = notes.mapIndexed { index, note ->
            "${index + 1}. ${note.title} (ID: ${note.id}) - ${note.content.take(50)}..."
        }.joinToString("\n")
        
        return ToolResult.Success("笔记列表：\n$list",
            mapOf("count" to notes.size, "ids" to notes.map { it.id }))
    }
}

class ListTimelineTool(
    private val timelineRepository: TimelineRepository
) : AgentTool {
    
    override val name = "list_timeline"
    override val description = "列出当前小说的所有时间线事件。"
    override val parameters = ToolParameters(
        properties = emptyMap(),
        required = emptyList()
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val events = timelineRepository.getTimelineByNovelId(context.novelId).first()
        
        if (events.isEmpty()) {
            return ToolResult.Success("当前小说暂无时间线事件")
        }
        
        val list = events.mapIndexed { index, event ->
            "${index + 1}. ${event.title} (ID: ${event.id}) - ${event.description.take(50)}..."
        }.joinToString("\n")
        
        return ToolResult.Success("时间线事件列表：\n$list",
            mapOf("count" to events.size, "ids" to events.map { it.id }))
    }
}
