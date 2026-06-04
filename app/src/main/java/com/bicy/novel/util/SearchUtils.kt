package com.bicy.novel.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 搜索优化工具
 * 提供防抖、高亮、模糊匹配等功能
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
object SearchUtils {
    
    /**
     * 创建防抖搜索流
     * @param searchQuery 搜索查询流
     * @param debounceMs 防抖延迟（毫秒）
     * @param searchFunction 搜索函数
     */
    fun <T> createDebouncedSearch(
        searchQuery: Flow<String>,
        debounceMs: Long = 300,
        searchFunction: suspend (String) -> List<T>
    ): Flow<List<T>> {
        return searchQuery
            .debounce(debounceMs)
            .distinctUntilChanged()
            .mapLatest { query ->
                if (query.isBlank()) {
                    emptyList()
                } else {
                    searchFunction(query)
                }
            }
            .catch { e ->
                emit(emptyList())
            }
    }
    
    /**
     * 高亮搜索关键词
     * @param text 原文本
     * @param keyword 关键词
     * @return 高亮后的文本（使用**包裹关键词）
     */
    fun highlightKeyword(text: String, keyword: String): String {
        if (keyword.isBlank()) return text
        
        val regex = Regex(Regex.escape(keyword), RegexOption.IGNORE_CASE)
        return regex.replace(text) { matchResult ->
            "**${matchResult.value}**"
        }
    }
    
    /**
     * 模糊匹配算法
     * @param text 目标文本
     * @param pattern 搜索模式
     * @return 是否匹配
     */
    fun fuzzyMatch(text: String, pattern: String): Boolean {
        if (pattern.isEmpty()) return true
        if (text.isEmpty()) return false
        
        val textLower = text.lowercase()
        val patternLower = pattern.lowercase()
        
        var patternIndex = 0
        for (char in textLower) {
            if (patternIndex < patternLower.length && char == patternLower[patternIndex]) {
                patternIndex++
            }
        }
        
        return patternIndex == patternLower.length
    }
    
    /**
     * 计算匹配得分
     * @param text 目标文本
     * @param pattern 搜索模式
     * @return 匹配得分（越高越匹配）
     */
    fun calculateMatchScore(text: String, pattern: String): Int {
        if (pattern.isEmpty()) return 0
        
        val textLower = text.lowercase()
        val patternLower = pattern.lowercase()
        
        // 完全匹配得分最高
        if (textLower == patternLower) return 1000
        
        // 开头匹配
        if (textLower.startsWith(patternLower)) return 800
        
        // 包含匹配
        if (textLower.contains(patternLower)) {
            val index = textLower.indexOf(patternLower)
            return 600 - index // 越靠前得分越高
        }
        
        // 模糊匹配
        if (fuzzyMatch(text, pattern)) {
            return 200
        }
        
        return 0
    }
    
    /**
     * 搜索并排序结果
     * @param items 待搜索项
     * @param pattern 搜索模式
     * @param keyExtractor 关键字提取函数
     * @return 排序后的搜索结果
     */
    fun <T> searchAndSort(
        items: List<T>,
        pattern: String,
        keyExtractor: (T) -> String
    ): List<T> {
        if (pattern.isBlank()) return items
        
        return items
            .map { item ->
                val score = calculateMatchScore(keyExtractor(item), pattern)
                Pair(item, score)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}

/**
 * 搜索结果高亮数据
 */
data class HighlightedText(
    val original: String,
    val highlighted: String,
    val ranges: List<IntRange>
) {
    companion object {
        fun create(text: String, keyword: String): HighlightedText {
            if (keyword.isBlank()) {
                return HighlightedText(text, text, emptyList())
            }
            
            val ranges = mutableListOf<IntRange>()
            val regex = Regex(Regex.escape(keyword), RegexOption.IGNORE_CASE)
            val highlighted = regex.replace(text) { matchResult ->
                ranges.add(matchResult.range)
                "**${matchResult.value}**"
            }
            
            return HighlightedText(text, highlighted, ranges)
        }
    }
}
