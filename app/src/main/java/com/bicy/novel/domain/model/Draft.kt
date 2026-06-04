package com.bicy.novel.domain.model

data class Draft(
    val targetType: String,
    val targetId: Long,
    val novelId: Long,
    val title: String,
    val content: String,
    val wordCount: Int,
    val savedAt: Long = System.currentTimeMillis()
)
