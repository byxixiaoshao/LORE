package com.bicy.novel.data.agent

import com.bicy.novel.domain.model.Chapter
import com.bicy.novel.domain.repository.ChapterRepository
import com.bicy.novel.util.WordCounter
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class MoveChapterTool(
    private val chapterRepository: ChapterRepository
) : AgentTool {
    
    override val name = "move_chapter"
    override val description = "移动章节顺序。可以将章节上移、下移或移动到指定位置。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "chapter_id" to ToolProperty("number", "要移动的章节ID"),
            "direction" to ToolProperty("string", "移动方向：up(上移), down(下移), to(移动到指定位置)",
                enum = listOf("up", "down", "to")),
            "target_position" to ToolProperty("number", "目标位置（当direction为to时使用）")
        ),
        required = listOf("chapter_id", "direction")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val chapterId = params.getLong("chapter_id")
        val direction = params.getString("direction")
        
        val chapter = chapterRepository.getChapterById(chapterId)
            ?: return ToolResult.Error("章节不存在")
        
        val chapters = chapterRepository.getChaptersByNovelId(context.novelId).first()
            .sortedBy { it.sortOrder }
        
        val currentIndex = chapters.indexOfFirst { it.id == chapterId }
        if (currentIndex == -1) return ToolResult.Error("章节不在当前小说中")
        
        val newIndex = when (direction) {
            "up" -> (currentIndex - 1).coerceIn(0, chapters.lastIndex)
            "down" -> (currentIndex + 1).coerceIn(0, chapters.lastIndex)
            "to" -> params.getInt("target_position").coerceIn(0, chapters.lastIndex)
            else -> return ToolResult.Error("未知的移动方向：$direction")
        }
        
        if (newIndex == currentIndex) {
            return ToolResult.Success("章节位置无需调整")
        }
        
        val mutableChapters = chapters.toMutableList()
        mutableChapters.removeAt(currentIndex)
        mutableChapters.add(newIndex, chapter)
        
        mutableChapters.forEachIndexed { index, ch ->
            if (ch.sortOrder != index + 1) {
                chapterRepository.updateChapter(ch.copy(sortOrder = index + 1))
            }
        }
        
        return ToolResult.Success("已将章节「${chapter.title}」从位置 ${currentIndex + 1} 移动到 ${newIndex + 1}")
    }
}

class MergeChaptersTool(
    private val chapterRepository: ChapterRepository
) : AgentTool {
    
    override val name = "merge_chapters"
    override val description = "合并多个章节为一个章节。章节内容将按顺序拼接。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "chapter_ids" to ToolProperty("string", "要合并的章节ID列表，用逗号分隔（按顺序合并）"),
            "new_title" to ToolProperty("string", "合并后的新章节标题（可选，默认使用第一个章节标题）"),
            "separator" to ToolProperty("string", "章节内容之间的分隔符（可选，默认为两个换行）")
        ),
        required = listOf("chapter_ids")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val chapterIds = params.getString("chapter_ids").split(",").map { it.trim().toLong() }
        val newTitle = if (params.has("new_title")) params.getString("new_title") else null
        val separator = params.optString("separator", "\n\n")
        
        if (chapterIds.size < 2) {
            return ToolResult.Error("至少需要两个章节才能合并")
        }
        
        val chapters = chapterIds.mapNotNull { chapterRepository.getChapterById(it) }
        if (chapters.size != chapterIds.size) {
            return ToolResult.Error("部分章节不存在")
        }
        
        val mergedContent = chapters.joinToString(separator) { it.content }
        val title = newTitle ?: chapters.first().title
        val firstChapter = chapters.first()
        
        val updatedChapter = firstChapter.copy(
            title = title,
            content = mergedContent,
            wordCount = WordCounter.count(mergedContent)
        )
        chapterRepository.updateChapter(updatedChapter)
        
        chapters.drop(1).forEach { chapterRepository.deleteChapter(it.id) }
        
        return ToolResult.Success("已合并 ${chapters.size} 个章节为「$title」，总字数：${updatedChapter.wordCount}",
            mapOf("id" to firstChapter.id, "wordCount" to updatedChapter.wordCount))
    }
}

class SplitChapterTool(
    private val chapterRepository: ChapterRepository
) : AgentTool {
    
    override val name = "split_chapter"
    override val description = "在指定位置拆分章节为两个章节。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "chapter_id" to ToolProperty("number", "要拆分的章节ID"),
            "split_position" to ToolProperty("number", "拆分位置（字符索引）"),
            "second_title" to ToolProperty("string", "拆分后第二个章节的标题（可选）")
        ),
        required = listOf("chapter_id", "split_position")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val chapterId = params.getLong("chapter_id")
        val splitPosition = params.getInt("split_position")
        val secondTitle = if (params.has("second_title")) params.getString("second_title") else null
        
        val chapter = chapterRepository.getChapterById(chapterId)
            ?: return ToolResult.Error("章节不存在")
        
        if (splitPosition <= 0 || splitPosition >= chapter.content.length) {
            return ToolResult.Error("拆分位置必须在章节内容中间")
        }
        
        val firstContent = chapter.content.substring(0, splitPosition)
        val secondContent = chapter.content.substring(splitPosition)
        
        val updatedChapter = chapter.copy(
            content = firstContent,
            wordCount = WordCounter.count(firstContent)
        )
        chapterRepository.updateChapter(updatedChapter)
        
        val newChapter = Chapter(
            novelId = context.novelId,
            title = secondTitle ?: "${chapter.title}（续）",
            content = secondContent,
            wordCount = WordCounter.count(secondContent),
            sortOrder = chapter.sortOrder + 1
        )
        val newId = chapterRepository.createChapter(newChapter)
        
        val chapters = chapterRepository.getChaptersByNovelId(context.novelId).first()
        chapters.filter { it.sortOrder > chapter.sortOrder && it.id != newId }.forEach { ch ->
            chapterRepository.updateChapter(ch.copy(sortOrder = ch.sortOrder + 1))
        }
        
        return ToolResult.Success("已将章节「${chapter.title}」拆分为两部分\n" +
            "第一部分：${updatedChapter.wordCount} 字\n" +
            "第二部分「${newChapter.title}」：${newChapter.wordCount} 字",
            mapOf("firstId" to chapterId, "secondId" to newId))
    }
}
