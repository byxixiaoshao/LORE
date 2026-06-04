package com.bicy.novel.data.agent

import com.bicy.novel.domain.repository.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class SearchTool(
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository
) : AgentTool {
    
    override val name = "search"
    override val description = "在指定范围内搜索内容。可以搜索章节、角色、世界观、笔记或全局搜索。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "query" to ToolProperty("string", "要搜索的关键词或内容"),
            "scope" to ToolProperty("string", "搜索范围：chapter(当前章节), chapters(所有章节), character(角色), worldview(世界观), note(笔记), global(全局)", 
                enum = listOf("chapter", "chapters", "character", "worldview", "note", "global")),
            "chapter_id" to ToolProperty("number", "当scope为chapter时，指定章节ID（可选，不指定则使用当前章节）")
        ),
        required = listOf("query", "scope")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val query = params.getString("query")
        val scope = params.getString("scope")
        
        val results = mutableListOf<String>()
        
        when (scope) {
            "chapter" -> {
                val chapterId = params.optLong("chapter_id", context.currentChapterId ?: -1)
                if (chapterId <= 0) {
                    return ToolResult.Error("未指定章节ID")
                }
                val chapter = chapterRepository.getChapterById(chapterId)
                if (chapter != null && chapter.content.contains(query, ignoreCase = true)) {
                    val indices = findAllIndices(chapter.content, query)
                    results.add("在章节「${chapter.title}」中找到 ${indices.size} 处匹配")
                    indices.take(5).forEach { index ->
                        val snippet = getSnippet(chapter.content, index, query.length)
                        results.add("  - 位置 $index: \"$snippet\"")
                    }
                }
            }
            "chapters" -> {
                val chapters = chapterRepository.getChaptersByNovelId(context.novelId).first()
                chapters.forEach { chapter ->
                    if (chapter.content.contains(query, ignoreCase = true)) {
                        val count = findAllIndices(chapter.content, query).size
                        results.add("章节「${chapter.title}」: $count 处匹配")
                    }
                }
            }
            "character" -> {
                val characters = characterRepository.getCharactersByNovelId(context.novelId).first()
                characters.forEach { char ->
                    if (char.name.contains(query, ignoreCase = true) || 
                        char.description.contains(query, ignoreCase = true)) {
                        results.add("角色「${char.name}」: ${char.description.take(100)}...")
                    }
                }
            }
            "worldview" -> {
                val worldviews = worldviewRepository.getWorldviewsByNovelId(context.novelId).first()
                worldviews.forEach { wv ->
                    if (wv.title.contains(query, ignoreCase = true) || 
                        wv.content.contains(query, ignoreCase = true)) {
                        results.add("世界观「${wv.title}」: ${wv.content.take(100)}...")
                    }
                }
            }
            "note" -> {
                val notes = noteRepository.getNotesByNovelId(context.novelId).first()
                notes.forEach { note ->
                    if (note.title.contains(query, ignoreCase = true) || 
                        note.content.contains(query, ignoreCase = true)) {
                        results.add("笔记「${note.title}」: ${note.content.take(100)}...")
                    }
                }
            }
            "global" -> {
                val chapters = chapterRepository.getChaptersByNovelId(context.novelId).first()
                chapters.forEach { chapter ->
                    if (chapter.content.contains(query, ignoreCase = true)) {
                        val count = findAllIndices(chapter.content, query).size
                        results.add("章节「${chapter.title}」: $count 处匹配")
                    }
                }
                val characters = characterRepository.getCharactersByNovelId(context.novelId).first()
                characters.forEach { char ->
                    if (char.name.contains(query, ignoreCase = true) || 
                        char.description.contains(query, ignoreCase = true)) {
                        results.add("角色「${char.name}」")
                    }
                }
            }
        }
        
        return if (results.isEmpty()) {
            ToolResult.Success("未找到匹配「$query」的内容")
        } else {
            ToolResult.Success("搜索结果：\n${results.joinToString("\n")}", 
                mapOf("count" to results.size))
        }
    }
    
    private fun findAllIndices(text: String, query: String): List<Int> {
        val indices = mutableListOf<Int>()
        var index = text.indexOf(query, ignoreCase = true)
        while (index >= 0) {
            indices.add(index)
            index = text.indexOf(query, index + 1, ignoreCase = true)
        }
        return indices
    }
    
    private fun getSnippet(text: String, index: Int, length: Int, contextSize: Int = 30): String {
        val start = maxOf(0, index - contextSize)
        val end = minOf(text.length, index + length + contextSize)
        return text.substring(start, end)
    }
}
