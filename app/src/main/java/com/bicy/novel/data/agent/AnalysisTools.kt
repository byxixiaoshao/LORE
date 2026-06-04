package com.bicy.novel.data.agent

import com.bicy.novel.domain.repository.ChapterRepository
import com.bicy.novel.util.WordCounter
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class CountWordsTool(
    private val chapterRepository: ChapterRepository
) : AgentTool {
    
    override val name = "count_words"
    override val description = "统计字数。可以统计当前章节或整部小说的字数。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "scope" to ToolProperty("string", "统计范围：current(当前章节), all(所有章节)",
                enum = listOf("current", "all")),
            "chapter_id" to ToolProperty("number", "指定章节ID（可选，不指定则使用当前章节）")
        ),
        required = listOf("scope")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val scope = params.getString("scope")
        
        return when (scope) {
            "current" -> {
                val chapterId = params.optLong("chapter_id", context.currentChapterId ?: -1)
                if (chapterId <= 0) {
                    return ToolResult.Error("未指定章节ID")
                }
                val chapter = chapterRepository.getChapterById(chapterId)
                    ?: return ToolResult.Error("章节不存在")
                
                val charCount = chapter.content.length
                val wordCount = WordCounter.count(chapter.content)
                val lineCount = chapter.content.lines().size
                
                ToolResult.Success("章节「${chapter.title}」统计：\n" +
                    "- 字符数：$charCount\n" +
                    "- 字数：$wordCount\n" +
                    "- 行数：$lineCount",
                    mapOf("charCount" to charCount, "wordCount" to wordCount, "lineCount" to lineCount))
            }
            "all" -> {
                val chapters = chapterRepository.getChaptersByNovelId(context.novelId).first()
                
                var totalChars = 0
                var totalWords = 0
                var totalLines = 0
                
                chapters.forEach { chapter ->
                    totalChars += chapter.content.length
                    totalWords += chapter.wordCount
                    totalLines += chapter.content.lines().size
                }
                
                ToolResult.Success("整部小说统计：\n" +
                    "- 章节数：${chapters.size}\n" +
                    "- 总字符数：$totalChars\n" +
                    "- 总字数：$totalWords\n" +
                    "- 总行数：$totalLines",
                    mapOf("chapterCount" to chapters.size, "charCount" to totalChars, 
                        "wordCount" to totalWords, "lineCount" to totalLines))
            }
            else -> ToolResult.Error("未知的统计范围：$scope")
        }
    }
}

class CheckConsistencyTool(
    private val chapterRepository: ChapterRepository,
    private val characterRepository: com.bicy.novel.domain.repository.CharacterRepository
) : AgentTool {
    
    override val name = "check_consistency"
    override val description = "检查文本一致性。检查角色名称在文中是否一致，以及可能的前后矛盾。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "check_type" to ToolProperty("string", "检查类型：character_names(角色名称), all(全部)",
                enum = listOf("character_names", "all"))
        ),
        required = listOf("check_type")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val checkType = params.getString("check_type")
        val issues = mutableListOf<String>()
        
        val characters = characterRepository.getCharactersByNovelId(context.novelId).first()
        val chapters = chapterRepository.getChaptersByNovelId(context.novelId).first()
        
        when (checkType) {
            "character_names", "all" -> {
                characters.forEach { character ->
                    val nameVariations = mutableSetOf<String>()
                    chapters.forEach { chapter ->
                        if (chapter.content.contains(character.name)) {
                            nameVariations.add(character.name)
                        }
                    }
                    
                    if (nameVariations.isEmpty()) {
                        issues.add("角色「${character.name}」在文中未出现")
                    }
                }
            }
        }
        
        return if (issues.isEmpty()) {
            ToolResult.Success("一致性检查通过，未发现问题")
        } else {
            ToolResult.Success("发现以下问题：\n${issues.joinToString("\n")}",
                mapOf("issueCount" to issues.size))
        }
    }
}
