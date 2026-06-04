package com.bicy.novel.domain.model

data class Note(
    val id: Long = 0,
    val novelId: Long? = null,
    val title: String,
    val content: String = "",
    val category: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
