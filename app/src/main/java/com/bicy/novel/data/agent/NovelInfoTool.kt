package com.bicy.novel.data.agent

import com.bicy.novel.domain.repository.NovelRepository
import com.bicy.novel.util.DateTimeUtils
import com.bicy.novel.util.WordCounter
import org.json.JSONObject

class NovelInfoTool(
    private val novelRepository: NovelRepository
) : AgentTool {
    
    override val name = "novel_info"
    override val description = "获取当前小说的基本信息，包括名称、简介、作者、分类、状态、字数、章节数等。"
    override val parameters = ToolParameters(
        properties = emptyMap(),
        required = emptyList()
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val novel = novelRepository.getNovelById(context.novelId)
            ?: return ToolResult.Error("小说不存在")
        
        val statusText = when (novel.status) {
            1 -> "已完成"
            else -> "连载中"
        }
        
        val info = buildString {
            appendLine("小说「${novel.title}」的基本信息：")
            if (novel.author.isNotEmpty()) {
                appendLine("- 作者：${novel.author}")
            }
            if (novel.description.isNotEmpty()) {
                appendLine("- 简介：${novel.description}")
            } else {
                appendLine("- 简介：（暂无简介）")
            }
            if (novel.category.isNotEmpty()) {
                appendLine("- 分类：${novel.category}")
            }
            appendLine("- 状态：$statusText")
            appendLine("- 总字数：${WordCounter.formatWordCount(novel.totalWords)}")
            appendLine("- 章节数：${novel.chapterCount} 章")
            appendLine("- 创建时间：${DateTimeUtils.formatDate(novel.createdAt)}")
            appendLine("- 最后更新：${DateTimeUtils.formatDate(novel.updatedAt)}")
        }.trimEnd()
        
        return ToolResult.Success(info, mapOf(
            "title" to novel.title,
            "author" to novel.author,
            "description" to novel.description,
            "category" to novel.category,
            "status" to novel.status,
            "totalWords" to novel.totalWords,
            "chapterCount" to novel.chapterCount,
            "createdAt" to novel.createdAt,
            "updatedAt" to novel.updatedAt
        ))
    }
}
