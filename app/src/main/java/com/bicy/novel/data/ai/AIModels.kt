package com.bicy.novel.data.ai

import kotlinx.coroutines.flow.Flow

data class ChatMessage(
    val role: String,
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null
)

data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val stream: Boolean = false,
    val tools: List<Map<String, Any>>? = null,
    val enableThinking: Boolean = false
)

data class ChatResponse(
    val id: String,
    val choices: List<ChatChoice>,
    val usage: ChatUsage? = null
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finishReason: String? = null
)

data class ChatUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

data class StreamChunk(
    val delta: ChatMessage,
    val finishReason: String? = null
)

interface AIProvider {
    suspend fun chat(request: ChatRequest): Result<ChatResponse>
    fun chatStream(request: ChatRequest): Flow<Result<StreamChunk>>
    suspend fun testConnection(): Result<Boolean>
}

sealed class AIResult<out T> {
    data class Success<T>(val data: T) : AIResult<T>()
    data class Error(val message: String, val code: Int? = null) : AIResult<Nothing>()
    data class Loading(val partial: String = "") : AIResult<Nothing>()
}
