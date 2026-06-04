package com.bicy.novel.domain.model

data class Chapter(
    val id: Long = 0,
    val novelId: Long,
    val volumeId: Long? = null,
    val title: String,
    val content: String = "",
    val wordCount: Int = 0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
