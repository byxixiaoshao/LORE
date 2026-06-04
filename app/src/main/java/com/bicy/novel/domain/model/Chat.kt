package com.bicy.novel.domain.model

data class ChatSession(
    val id: Long = 0,
    val novelId: Long,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ChatMsg(
    val id: Long = 0,
    val sessionId: Long,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isUser(): Boolean = role == "user"
}
