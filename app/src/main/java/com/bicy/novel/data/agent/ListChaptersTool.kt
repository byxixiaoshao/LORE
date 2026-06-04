package com.bicy.novel.data.agent

import com.bicy.novel.domain.repository.ChapterRepository
import org.json.JSONObject

class ListChaptersTool(
    private val chapterRepository: ChapterRepository
) : AgentTool {
    
    override val name = "list_chapters"
    override val description = "列出小说的所有章节，返回章节ID、标题和字数。用于了解章节结构。"
    override val parameters = ToolParameters(
        properties = emptyMap(),
        required = emptyList()
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val chapters = chapterRepository.getChaptersByNovelIdOnce(context.novelId)
        
        if (chapters.isEmpty()) {
            return ToolResult.Success("当前小说暂无章节")
        }
        
        val sb = StringBuilder()
        sb.append("章节列表：\n")
        chapters.forEach { chapter ->
            sb.append("  - ID: ${chapter.id}, 标题: 「${chapter.title}」, 字数: ${chapter.wordCount}, 排序: ${chapter.sortOrder}\n")
        }
        
        return ToolResult.Success(sb.toString().trimEnd())
    }
}
