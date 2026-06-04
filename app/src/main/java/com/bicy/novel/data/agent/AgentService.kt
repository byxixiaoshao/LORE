package com.bicy.novel.data.agent

import com.bicy.novel.domain.repository.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentService @Inject constructor(
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository
) {
    private val tools: Map<String, AgentTool> by lazy {
        mapOf(
            "novel_info" to NovelInfoTool(novelRepository),
            "search" to SearchTool(chapterRepository, characterRepository, worldviewRepository, noteRepository),
            "view" to ViewTool(chapterRepository, characterRepository, worldviewRepository, noteRepository, timelineRepository),
            "create" to CreateTool(chapterRepository, characterRepository, worldviewRepository, noteRepository, timelineRepository),
            "edit" to EditTool(chapterRepository, characterRepository, worldviewRepository, noteRepository, timelineRepository),
            "delete" to DeleteTool(chapterRepository, characterRepository, worldviewRepository, noteRepository, timelineRepository),
            "move_chapter" to MoveChapterTool(chapterRepository),
            "merge_chapters" to MergeChaptersTool(chapterRepository),
            "split_chapter" to SplitChapterTool(chapterRepository),
            "list_chapters" to ListChaptersTool(chapterRepository),
            "list_characters" to ListCharactersTool(characterRepository),
            "list_worldviews" to ListWorldviewsTool(worldviewRepository),
            "list_notes" to ListNotesTool(noteRepository),
            "list_timeline" to ListTimelineTool(timelineRepository),
            "count_words" to CountWordsTool(chapterRepository),
            "check_consistency" to CheckConsistencyTool(chapterRepository, characterRepository),
            "rewrite_selection" to RewriteSelectionTool(),
            "polish_selection" to PolishSelectionTool(),
            "generate_outline" to GenerateOutlineTool()
        )
    }
    
    fun getToolsDefinition(): List<Map<String, Any>> {
        return tools.values.map { it.toOpenAITool() }
    }
    
    suspend fun executeTool(
        toolName: String,
        arguments: JSONObject,
        context: ToolContext
    ): ToolResult {
        val tool = tools[toolName]
            ?: return ToolResult.Error("未知的工具：$toolName")
        
        return try {
            tool.execute(arguments, context)
        } catch (e: Exception) {
            ToolResult.Error("工具执行失败：${e.message}")
        }
    }
    
    fun getToolDescription(toolName: String): String? {
        return tools[toolName]?.description
    }
    
    fun listAvailableTools(): List<String> {
        return tools.keys.toList()
    }
}
