package com.bicy.novel.util

object WordCounter {
    
    fun count(text: String): Int {
        if (text.isBlank()) return 0
        
        val cleanedText = text
            .replace(Regex("\\s+"), "")
            .replace(Regex("[\\p{Punct}\\p{S}]"), "")
        
        return cleanedText.length
    }
    
    fun countChinese(text: String): Int {
        if (text.isBlank()) return 0
        
        val chineseRegex = Regex("[\\u4e00-\\u9fa5]")
        return chineseRegex.findAll(text).count()
    }
    
    fun countWithPunctuation(text: String): Int {
        if (text.isBlank()) return 0
        return text.replace(Regex("\\s+"), "").length
    }
    
    fun formatWordCount(count: Int): String {
        return when {
            count >= 10000 -> String.format("%.1f万字", count / 10000.0)
            else -> "${count}字"
        }
    }
}
