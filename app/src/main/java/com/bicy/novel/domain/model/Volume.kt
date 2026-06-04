package com.bicy.novel.domain.model

data class Volume(
    val id: Long = 0,
    val novelId: Long,
    val title: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
