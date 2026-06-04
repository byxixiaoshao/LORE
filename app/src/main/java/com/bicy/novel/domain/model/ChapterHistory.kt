package com.bicy.novel.domain.model

data class ContentHistory(
    val id: Long = 0,
    val targetType: String, // "chapter", "character", "worldview", "note", "timeline"
    val targetId: Long,
    val title: String,
    val content: String,
    val wordCount: Int,
    val savedAt: Long,
    val note: String? = null
)
