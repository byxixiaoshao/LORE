package com.bicy.novel.data.ai

import android.content.Context
import android.util.Log
import com.bicy.novel.data.agent.AgentService
import com.bicy.novel.data.agent.ToolContext
import com.bicy.novel.data.agent.ToolResult
import com.bicy.novel.data.preferences.AppSettings
import com.bicy.novel.util.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class AIService(private val context: Context) {
    private var provider: AIProvider? = null
    private var currentSettings: AppSettings? = null
    
    companion object {
        private const val TAG = "AIService"
    }
    
    fun initialize(settings: AppSettings) {
        currentSettings = settings
        if (settings.aiEnabled && settings.aiApiKey.isNotBlank() && settings.aiApiEndpoint.isNotBlank()) {
            provider = OpenAIProvider(
                apiKey = settings.aiApiKey,
                baseUrl = settings.aiApiEndpoint,
                maxRetries = 5,
                connectTimeout = 30000,
                readTimeout = 120000
            )
        } else {
            provider = null
        }
    }
    
    fun isInitialized(): Boolean = provider != null && currentSettings?.aiEnabled == true
    
    suspend fun chat(
        messages: List<ChatMessage>,
        temperature: Float? = null,
        maxTokens: Int? = null
    ): Result<ChatResponse> {
        val settings = currentSettings
        if (settings == null) {
            return Result.failure(Exception("AI 未初始化：请先配置 AI 设置"))
        }
        if (!settings.aiEnabled) {
            return Result.failure(Exception("AI 功能未启用：请在设置中启用 AI"))
        }
        if (settings.aiApiKey.isBlank()) {
            return Result.failure(Exception("API Key 未配置：请在设置中填写 API Key"))
        }
        if (settings.aiApiEndpoint.isBlank()) {
            return Result.failure(Exception("API 端点未配置：请在设置中填写 API 端点"))
        }
        
        val p = provider ?: return Result.failure(Exception("AI 未初始化"))
        
        val request = ChatRequest(
            model = settings.aiModel,
            messages = messages,
            temperature = temperature ?: settings.aiTemperature,
            maxTokens = maxTokens ?: settings.aiMaxTokens,
            enableThinking = settings.aiEnableThinking
        )
        
        return p.chat(request)
    }
    
    data class ToolCallInfo(
        val toolName: String,
        val arguments: String,
        val result: String? = null,
        val isError: Boolean = false,
        val isComplete: Boolean = false
    )
    
    data class OperationRecord(
        val operationType: String,
        val targetType: String,
        val targetId: Long,
        val targetName: String,
        val beforeData: String?,
        val afterData: String?
    )
    
    data class RoundInfo(
        val round: Int,
        val thinking: String?,
        val hasToolCalls: Boolean
    )
    
    suspend fun chatWithTools(
        messages: MutableList<ChatMessage>,
        agentService: AgentService,
        toolContext: ToolContext,
        temperature: Float? = null,
        maxTokens: Int? = null,
        onRound: ((RoundInfo) -> Unit)? = null,
        onToolCall: ((ToolCallInfo) -> Unit)? = null,
        onOperation: ((OperationRecord) -> Unit)? = null
    ): Result<ChatResponse> {
        val settings = currentSettings
        if (settings == null) {
            return Result.failure(Exception("AI 未初始化：请先配置 AI 设置"))
        }
        if (!settings.aiEnabled) {
            return Result.failure(Exception("AI 功能未启用：请在设置中启用 AI"))
        }
        
        val p = provider ?: return Result.failure(Exception("AI 未初始化"))
        val tools = agentService.getToolsDefinition()
        val maxIterations = settings.aiMaxToolIterations.coerceIn(1, 100)
        
        var iteration = 0
        while (iteration < maxIterations) {
            iteration++
            Log.d(TAG, "Agent 迭代 $iteration")
            
            var networkWaitCount = 0
            while (!NetworkUtils.isNetworkAvailable(context) && networkWaitCount < 10) {
                networkWaitCount++
                Log.w(TAG, "网络不可用，等待恢复... ($networkWaitCount/10)")
                onToolCall?.invoke(ToolCallInfo(
                    toolName = "网络检测",
                    arguments = "",
                    result = "等待网络恢复中... ($networkWaitCount/10)",
                    isComplete = true,
                    isError = false
                ))
                delay(2000)
            }
            
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return Result.failure(Exception("网络不可用：请检查网络连接"))
            }
            
            val request = ChatRequest(
                model = settings.aiModel,
                messages = messages,
                temperature = temperature ?: settings.aiTemperature,
                maxTokens = maxTokens ?: settings.aiMaxTokens,
                tools = tools,
                enableThinking = settings.aiEnableThinking
            )
            
            val result = p.chat(request)
            if (result.isFailure) {
                return result
            }
            
            val response = result.getOrNull()!!
            val message = response.choices.firstOrNull()?.message
                ?: return Result.failure(Exception("无响应消息"))
            
            val thinkingContent = extractThinkingContent(message.content)
            val hasToolCalls = !message.toolCalls.isNullOrEmpty()
            
            onRound?.invoke(RoundInfo(
                round = iteration,
                thinking = thinkingContent,
                hasToolCalls = hasToolCalls
            ))
            
            if (!hasToolCalls) {
                return Result.success(response)
            }
            
            messages.add(message)
            
            for (toolCall in message.toolCalls) {
                val toolName = toolCall.function.name
                val toolArgs = toolCall.function.arguments
                Log.d(TAG, "工具调用: $toolName($toolArgs)")
                
                onToolCall?.invoke(ToolCallInfo(
                    toolName = toolName,
                    arguments = toolArgs,
                    isComplete = false
                ))
                
                val argsJson = try {
                    JSONObject(toolArgs)
                } catch (e: Exception) {
                    Log.e(TAG, "工具参数JSON解析失败，可能被截断: ${toolArgs.take(200)}...", e)
                    return Result.failure(Exception("工具参数被截断，请尝试减少内容长度或增加最大Token值"))
                }
                val toolResult = agentService.executeTool(toolName, argsJson, toolContext)
                
                val (toolResultContent, isError) = when (toolResult) {
                    is ToolResult.Success -> {
                        if (toolResult.hasOperation()) {
                            onOperation?.invoke(OperationRecord(
                                operationType = toolResult.operationType!!,
                                targetType = toolResult.targetType!!,
                                targetId = toolResult.targetId!!,
                                targetName = toolResult.targetName ?: "",
                                beforeData = toolResult.beforeData,
                                afterData = toolResult.afterData
                            ))
                        }
                        Pair(toolResult.message, false)
                    }
                    is ToolResult.Error -> Pair("错误: ${toolResult.message}", true)
                }
                
                Log.d(TAG, "工具结果: $toolResultContent")
                
                onToolCall?.invoke(ToolCallInfo(
                    toolName = toolName,
                    arguments = toolArgs,
                    result = toolResultContent,
                    isError = isError,
                    isComplete = true
                ))
                
                messages.add(ChatMessage(
                    role = "tool",
                    content = toolResultContent,
                    toolCallId = toolCall.id
                ))
            }
        }
        
        return Result.failure(Exception("超过最大工具调用次数 ($maxIterations 次)，请在设置中调整"))
    }
    
    private fun extractThinkingContent(content: String): String? {
        val thinkingStartTag = "\u2354"
        val thinkingEndTag = "\u2355"
        
        val start = content.indexOf(thinkingStartTag)
        val end = content.indexOf(thinkingEndTag)
        
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start + 1, end).trim().ifEmpty { null }
        } else if (start != -1) {
            return content.substring(start + 1).trim().ifEmpty { null }
        }
        
        val altStart = content.indexOf("<tool_call>")
        val altEnd = content.indexOf("⋟")
        if (altStart != -1 && altEnd != -1 && altEnd > altStart) {
            return content.substring(altStart + 2, altEnd).trim().ifEmpty { null }
        }
        
        val cnStart = content.indexOf("【思考】")
        val cnEnd = content.indexOf("【/思考】")
        if (cnStart != -1 && cnEnd != -1 && cnEnd > cnStart) {
            return content.substring(cnStart + 4, cnEnd).trim().ifEmpty { null }
        }
        
        return null
    }
    
    fun chatStream(
        messages: List<ChatMessage>,
        temperature: Float? = null,
        maxTokens: Int? = null
    ): Flow<Result<StreamChunk>> {
        val settings = currentSettings
        if (settings == null) {
            return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("AI 未初始化")))
        }
        if (!settings.aiEnabled) {
            return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("AI 功能未启用")))
        }
        if (settings.aiApiKey.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("API Key 未配置")))
        }
        if (settings.aiApiEndpoint.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("API 端点未配置")))
        }
        
        val p = provider ?: return kotlinx.coroutines.flow.flowOf(Result.failure(Exception("AI 未初始化")))
        
        val request = ChatRequest(
            model = settings.aiModel,
            messages = messages,
            temperature = temperature ?: settings.aiTemperature,
            maxTokens = maxTokens ?: settings.aiMaxTokens,
            stream = true,
            enableThinking = settings.aiEnableThinking
        )
        
        return p.chatStream(request)
    }
    
    suspend fun testConnection(): Result<Boolean> {
        val p = provider ?: return Result.failure(Exception("AI not initialized"))
        return p.testConnection()
    }
    
    fun buildSystemPrompt(context: AIContext, isAgent: Boolean = false): String {
        val settings = currentSettings
        val sb = StringBuilder()
        
        if (isAgent) {
            val agentPrompt = settings?.aiAgentSystemPrompt ?: AppSettings.DEFAULT_AGENT_SYSTEM_PROMPT
            sb.append(agentPrompt)
            sb.append("\n\n")
            
            sb.append("【重要】当前小说ID: ${context.novelId}\n")
            if (context.currentChapterId > 0) {
                sb.append("【重要】当前章节ID: ${context.currentChapterId} (标题: ${context.currentChapterTitle})\n")
            }
            sb.append("\n在使用 view、edit、delete 等工具时，请使用上述ID，不要猜测ID值。\n")
            sb.append("如果需要查看其他章节，请先使用 list_chapters 工具获取所有章节ID。\n\n")
        } else {
            val normalPrompt = settings?.aiSystemPrompt ?: AppSettings.DEFAULT_SYSTEM_PROMPT
            sb.append(normalPrompt)
            sb.append("\n\n")
        }
        
        sb.append("以下是当前小说的背景信息：\n\n")
        
        if (context.novelTitle.isNotBlank()) {
            sb.append("小说标题：${context.novelTitle}\n")
        }
        
        if (context.chaptersSummary.isNotEmpty()) {
            sb.append("章节列表：\n")
            context.chaptersSummary.forEach { ch ->
                sb.append("  - ID: ${ch.id}, 标题: 「${ch.title}」, 字数: ${ch.wordCount}\n")
            }
            sb.append("\n")
        }
        
        if (context.currentChapter.isNotBlank()) {
            sb.append("当前章节内容：\n${context.currentChapter}\n\n")
        }
        
        if (context.characters.isNotEmpty()) {
            sb.append("角色信息：\n")
            context.characters.forEach { char ->
                sb.append("- $char\n")
            }
            sb.append("\n")
        }
        
        if (context.worldview.isNotBlank()) {
            sb.append("世界观设定：\n${context.worldview}\n\n")
        }
        
        if (context.notes.isNotEmpty()) {
            sb.append("笔记：\n")
            context.notes.forEach { note ->
                sb.append("- $note\n")
            }
            sb.append("\n")
        }
        
        if (isAgent) {
            sb.append("请使用工具来帮助用户完成小说创作任务。")
        } else {
            sb.append("请根据以上信息协助用户进行小说创作。")
        }
        
        return sb.toString()
    }
}

data class AIContext(
    val novelTitle: String = "",
    val novelId: Long = 0,
    val currentChapterId: Long = 0,
    val currentChapterTitle: String = "",
    val currentChapter: String = "",
    val characters: List<String> = emptyList(),
    val worldview: String = "",
    val notes: List<String> = emptyList(),
    val chaptersSummary: List<ChapterSummary> = emptyList()
)

data class ChapterSummary(
    val id: Long,
    val title: String,
    val wordCount: Int,
    val sortOrder: Int
)
