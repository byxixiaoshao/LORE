package com.bicy.novel.domain.model

data class TimelineEvent(
    val id: Long = 0,
    val novelId: Long,
    val title: String,
    val description: String = "",
    val eventDate: String = "",
    val sortOrder: Int = 0,
    val isKeyEvent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
