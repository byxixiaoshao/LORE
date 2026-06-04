package com.bicy.novel.ui.screens

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.data.ai.AIService
import com.bicy.novel.data.ai.AIContext
import com.bicy.novel.data.ai.ChatMessage as AIChatMessage
import com.bicy.novel.data.agent.AgentService
import com.bicy.novel.data.agent.ToolContext
import com.bicy.novel.data.local.entity.AIOperationEntity
import com.bicy.novel.data.preferences.AppSettings
import com.bicy.novel.data.preferences.SettingsPreferences
import com.bicy.novel.domain.model.ChatSession
import com.bicy.novel.domain.model.ChatMsg
import com.bicy.novel.domain.model.Chapter
import com.bicy.novel.domain.model.Character
import com.bicy.novel.domain.model.Worldview
import com.bicy.novel.domain.model.Note
import com.bicy.novel.domain.model.TimelineEvent
import com.bicy.novel.domain.repository.ChatRepository
import com.bicy.novel.domain.repository.ChapterRepository
import com.bicy.novel.domain.repository.CharacterRepository
import com.bicy.novel.domain.repository.WorldviewRepository
import com.bicy.novel.domain.repository.NoteRepository
import com.bicy.novel.domain.repository.TimelineRepository
import com.bicy.novel.service.AIRequestService
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StreamingRound(
    val round: Int,
    val thinking: String = "",
    val toolCalls: List<ToolCallEntry> = emptyList(),
    val isComplete: Boolean = false
)

data class StreamingMessage(
    val rounds: List<StreamingRound> = emptyList(),
    val content: String = "",
    val isComplete: Boolean = false,
    val updateVersion: Long = 0
) {
    fun getCurrentRound(): StreamingRound {
        return rounds.lastOrNull() ?: StreamingRound(round = 0)
    }
    
    fun withNewRound(round: Int, thinking: String = "", hasToolCalls: Boolean = true): StreamingMessage {
        val newRound = StreamingRound(
            round = round,
            thinking = thinking,
            isComplete = !hasToolCalls
        )
        return copy(rounds = rounds + newRound, updateVersion = updateVersion + 1)
    }
    
    fun updateCurrentRound(thinking: String? = null, isComplete: Boolean? = null): StreamingMessage {
        if (rounds.isEmpty()) return this
        val currentRound = rounds.last()
        val updatedRound = currentRound.copy(
            thinking = thinking ?: currentRound.thinking,
            isComplete = isComplete ?: currentRound.isComplete
        )
        return copy(rounds = rounds.dropLast(1) + updatedRound, updateVersion = updateVersion + 1)
    }
    
    fun addToolCall(toolCall: ToolCallEntry): StreamingMessage {
        if (rounds.isEmpty()) return this
        val currentRound = rounds.last()
        val updatedRound = currentRound.copy(
            toolCalls = currentRound.toolCalls + toolCall
        )
        return copy(rounds = rounds.dropLast(1) + updatedRound, updateVersion = updateVersion + 1)
    }
    
    fun updateToolCall(toolCallIndex: Int, toolCall: ToolCallEntry): StreamingMessage {
        if (rounds.isEmpty()) return this
        val currentRound = rounds.last()
        val updatedToolCalls = currentRound.toolCalls.toMutableList()
        if (toolCallIndex in updatedToolCalls.indices) {
            updatedToolCalls[toolCallIndex] = toolCall
        }
        val updatedRound = currentRound.copy(toolCalls = updatedToolCalls)
        return copy(rounds = rounds.dropLast(1) + updatedRound, updateVersion = updateVersion + 1)
    }
    
    fun appendContent(text: String): StreamingMessage {
        return copy(content = content + text, updateVersion = updateVersion + 1)
    }
    
    fun markComplete(): StreamingMessage {
        return copy(isComplete = true, updateVersion = updateVersion + 1)
    }
}

data class ToolCallEntry(
    val toolName: String,
    val arguments: String,
    val result: String? = null,
    val isError: Boolean = false,
    val isComplete: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class AIChatUiState(
    val currentSessionId: Long = 0,
    val messages: List<ChatMsg> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAiEnabled: Boolean = false,
    val sessions: List<ChatSession> = emptyList(),
    val expandedSegments: Map<Long, Set<Int>> = emptyMap(),
    val selectedText: String? = null,
    val selectedRange: IntRange? = null,
    val currentChapterId: Long? = null,
    val pendingDeleteRequest: PendingDeleteRequest? = null,
    val currentRound: Int = 0,
    val refreshTrigger: Long = 0
)

data class PendingDeleteRequest(
    val toolName: String,
    val targetType: String,
    val targetId: Long,
    val targetName: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

@HiltViewModel
class AIChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatRepository: ChatRepository,
    private val settingsPreferences: SettingsPreferences,
    private val aiService: AIService,
    private val agentService: AgentService,
    private val aiOperationRepository: com.bicy.novel.domain.repository.AIOperationRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "AIChatViewModel"
    }
    
    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _streamingMessage = MutableStateFlow<StreamingMessage?>(null)
    val streamingMessage = _streamingMessage.asStateFlow()
    
    private val _streamingExpanded = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val streamingExpanded = _streamingExpanded.asStateFlow()
    
    private var currentNovelId: Long = 0
    private var currentSettings: AppSettings? = null
    private var isStopped = false
    
    fun toggleStreamingExpand(index: Int) {
        _streamingExpanded.value = _streamingExpanded.value.toMutableMap().apply {
            this[index] = !(this[index] ?: false)
        }
    }
    
    fun stopGeneration() {
        isStopped = true
        AIRequestService.stop(context)
        _streamingMessage.value = null
        _uiState.update { it.copy(isLoading = false, error = "已停止") }
    }
    
    init {
        viewModelScope.launch {
            settingsPreferences.settings.collect { settings ->
                currentSettings = settings
                _uiState.update { it.copy(isAiEnabled = settings.aiEnabled) }
                aiService.initialize(settings)
            }
        }
    }
    
    fun setNovelId(novelId: Long) {
        if (currentNovelId == novelId) return
        currentNovelId = novelId
        
        viewModelScope.launch {
            chatRepository.getSessionsByNovelId(novelId).collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
            }
        }
    }
    
    fun startNewSession() {
        viewModelScope.launch {
            val session = ChatSession(
                novelId = currentNovelId,
                title = "新对话 - ${formatCurrentTime()}"
            )
            val sessionId = chatRepository.createSession(session)
            _uiState.update { 
                it.copy(
                    currentSessionId = sessionId,
                    messages = emptyList()
                )
            }
        }
    }
    
    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentSessionId = sessionId, messages = emptyList()) }
            chatRepository.getMessagesBySessionOnce(sessionId).let { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }
    
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_uiState.value.currentSessionId == sessionId) {
                _uiState.update { 
                    it.copy(
                        currentSessionId = 0,
                        messages = emptyList()
                    )
                }
            }
        }
    }
    
    fun toggleSegment(messageId: Long, segmentIndex: Int) {
        _uiState.update { state ->
            val messageSegments = state.expandedSegments[messageId] ?: emptySet()
            val newSegments = if (messageSegments.contains(segmentIndex)) {
                messageSegments - segmentIndex
            } else {
                messageSegments + segmentIndex
            }
            state.copy(
                expandedSegments = state.expandedSegments + (messageId to newSegments)
            )
        }
    }
    
    fun getExpandedSegments(messageId: Long): Set<Int> {
        return _uiState.value.expandedSegments[messageId] ?: emptySet()
    }
    
    fun setSelectedText(text: String?, range: IntRange? = null) {
        _uiState.update { it.copy(selectedText = text, selectedRange = range) }
    }
    
    fun setCurrentChapterId(chapterId: Long?) {
        _uiState.update { it.copy(currentChapterId = chapterId) }
    }
    
    fun clearSelectedText() {
        _uiState.update { it.copy(selectedText = null, selectedRange = null) }
    }
    
    fun sendMessage(
        content: String,
        context: AIContext,
        isAgent: Boolean = false
    ) {
        if (content.isBlank()) return
        
        val settings = currentSettings
        if (settings == null) {
            _uiState.update { it.copy(error = "设置未加载，请稍后重试") }
            return
        }
        if (!settings.aiEnabled) {
            _uiState.update { it.copy(error = "AI 功能未启用，请在设置中启用") }
            return
        }
        if (settings.aiApiKey.isBlank()) {
            _uiState.update { it.copy(error = "API Key 未配置") }
            return
        }
        if (settings.aiApiEndpoint.isBlank()) {
            _uiState.update { it.copy(error = "API 端点未配置") }
            return
        }
        
        aiService.initialize(settings)
        
        val state = _uiState.value
        val selectedText = state.selectedText
        val finalContent = if (!selectedText.isNullOrBlank()) {
            "$content\n\n[选中内容]\n$selectedText\n[/选中内容]"
        } else {
            content
        }
        
        viewModelScope.launch {
            val sessionId = state.currentSessionId
            if (sessionId == 0L) {
                val newSession = ChatSession(
                    novelId = currentNovelId,
                    title = content.take(30) + if (content.length > 30) "..." else ""
                )
                val newSessionId = chatRepository.createSession(newSession)
                _uiState.update { it.copy(currentSessionId = newSessionId) }
                if (isAgent) {
                    sendMessageWithAgent(newSessionId, finalContent, context)
                } else {
                    sendMessageStreaming(newSessionId, finalContent, context)
                }
            } else {
                if (isAgent) {
                    sendMessageWithAgent(sessionId, finalContent, context)
                } else {
                    sendMessageStreaming(sessionId, finalContent, context)
                }
            }
            
            clearSelectedText()
        }
    }
    
    private suspend fun sendMessageWithAgent(
        sessionId: Long,
        content: String,
        aiContext: AIContext
    ) {
        AIRequestService.start(context)
        isStopped = false
        _streamingExpanded.value = emptyMap()
        _streamingMessage.value = StreamingMessage()
        _uiState.update { it.copy(isLoading = true, error = null, currentRound = 0) }
        
        val userMessage = ChatMsg(
            sessionId = sessionId,
            role = "user",
            content = content
        )
        chatRepository.addMessage(userMessage)
        
        val updatedMessages = chatRepository.getMessagesBySessionOnce(sessionId)
        _uiState.update { it.copy(messages = updatedMessages) }
        
        val historyMessages = chatRepository.getMessagesBySessionOnce(sessionId)
        
        val aiMessages = mutableListOf<AIChatMessage>()
        aiMessages.add(AIChatMessage("system", aiService.buildSystemPrompt(aiContext, isAgent = true)))
        historyMessages.forEach { msg ->
            aiMessages.add(AIChatMessage(msg.role, msg.content))
        }
        
        val operationsList = mutableListOf<AIOperationEntity>()
        var lastAssistantMessageId: Long = 0
        
        try {
            val result = aiService.chatWithTools(
                messages = aiMessages,
                agentService = agentService,
                toolContext = ToolContext(
                    novelId = currentNovelId,
                    currentChapterId = _uiState.value.currentChapterId,
                    selectedText = _uiState.value.selectedText,
                    selectedRange = _uiState.value.selectedRange
                ),
                onRound = { roundInfo ->
                    if (isStopped) return@chatWithTools
                    var msg = _streamingMessage.value ?: StreamingMessage()
                    val currentRound = msg.rounds.find { it.round == roundInfo.round }
                    if (currentRound == null) {
                        msg = msg.withNewRound(roundInfo.round, roundInfo.thinking ?: "", roundInfo.hasToolCalls)
                    } else {
                        msg = msg.updateCurrentRound(
                            thinking = roundInfo.thinking ?: currentRound.thinking,
                            isComplete = if (!roundInfo.hasToolCalls) true else currentRound.isComplete
                        )
                    }
                    _streamingMessage.value = msg
                    _uiState.update { it.copy(currentRound = roundInfo.round) }
                },
                onToolCall = { info ->
                    if (isStopped) return@chatWithTools
                    var msg = _streamingMessage.value ?: StreamingMessage()
                    if (msg.rounds.isEmpty()) {
                        msg = msg.withNewRound(1, "", true)
                    }
                    val currentRound = msg.getCurrentRound()
                    if (info.isComplete) {
                        val existingIndex = currentRound.toolCalls.indexOfFirst { 
                            it.toolName == info.toolName && it.arguments == info.arguments && !it.isComplete 
                        }
                        val newToolCall = ToolCallEntry(
                            toolName = info.toolName,
                            arguments = info.arguments,
                            result = info.result,
                            isError = info.isError,
                            isComplete = true
                        )
                        msg = if (existingIndex >= 0) {
                            msg.updateToolCall(existingIndex, newToolCall)
                        } else {
                            msg.addToolCall(newToolCall)
                        }
                    } else {
                        msg = msg.addToolCall(ToolCallEntry(
                            toolName = info.toolName,
                            arguments = info.arguments,
                            isComplete = false
                        ))
                    }
                    _streamingMessage.value = msg
                },
                onOperation = { op ->
                    operationsList.add(AIOperationEntity(
                        sessionId = sessionId,
                        messageId = 0,
                        operationType = op.operationType,
                        targetType = op.targetType,
                        targetId = op.targetId,
                        targetName = op.targetName,
                        beforeData = op.beforeData,
                        afterData = op.afterData
                    ))
                }
            )
            
            if (isStopped) {
                _streamingMessage.value = null
                _uiState.update { it.copy(isLoading = false) }
                AIRequestService.stop(context)
                return
            }
            
            result.fold(
                onSuccess = { response ->
                    val assistantContent = response.choices.firstOrNull()?.message?.content ?: ""
                    val cleanContent = removeThinkingTags(assistantContent)
                    
                    Log.d(TAG, "Agent响应: assistantContent=${assistantContent.take(200)}, cleanContent=${cleanContent.take(200)}")
                    
                    var msg = _streamingMessage.value ?: StreamingMessage()
                    msg = msg.appendContent(cleanContent).copy(isComplete = true)
                    _streamingMessage.value = msg
                    
                    val roundsSummary = buildRoundsSummary(msg)
                    Log.d(TAG, "保存消息: rounds=${msg.rounds.size}, toolCalls=${msg.rounds.sumOf { it.toolCalls.size }}")
                    Log.d(TAG, "roundsSummary长度: ${roundsSummary.length}, 包含工具调用结束: ${roundsSummary.contains("[工具调用结束]")}")
                    Log.d(TAG, "roundsSummary内容: $roundsSummary")
                    
                    if (cleanContent.isNotBlank() || roundsSummary.isNotEmpty()) {
                        val assistantMessage = ChatMsg(
                            sessionId = sessionId,
                            role = "assistant",
                            content = roundsSummary + cleanContent
                        )
                        lastAssistantMessageId = chatRepository.addMessage(assistantMessage)
                        
                        if (operationsList.isNotEmpty()) {
                            val operationsWithMessageId = operationsList.map { it.copy(messageId = lastAssistantMessageId) }
                            aiOperationRepository.insertAll(operationsWithMessageId)
                        }
                    }
                    
                    val finalMessages = chatRepository.getMessagesBySessionOnce(sessionId)
                    _streamingMessage.value = null
                    _uiState.update { 
                        it.copy(
                            messages = finalMessages,
                            isLoading = false,
                            refreshTrigger = it.refreshTrigger + 1
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Agent 调用失败", error)
                    _streamingMessage.value = null
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Agent 异常", e)
            _streamingMessage.value = null
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
        
        AIRequestService.stop(context)
        clearSelectedText()
    }
    
    private fun removeThinkingTags(content: String): String {
        var result = content
        
        val tagPairs = listOf(
            Pair("\u2354", "\u2355"),
            Pair("<think>", "</think>"),
            Pair("【思考】", "【/思考】")
        )
        
        for ((startTag, endTag) in tagPairs) {
            while (true) {
                val start = result.indexOf(startTag)
                val end = result.indexOf(endTag)
                
                if (start != -1 && end != -1 && end > start) {
                    result = result.removeRange(start, end + endTag.length)
                } else if (start != -1) {
                    result = result.removeRange(start, start + startTag.length)
                } else {
                    break
                }
            }
        }
        
        return result.trim()
    }
    
    private fun buildRoundsSummary(streamingMsg: StreamingMessage): String {
        val sb = StringBuilder()
        streamingMsg.rounds.forEach { round ->
            if (round.thinking.isNotEmpty()) {
                sb.append("\u2354").append(round.thinking).append("\u2355")
            }
            if (round.toolCalls.isNotEmpty()) {
                sb.append("\n[工具调用开始]\n")
                round.toolCalls.forEach { tc ->
                    val status = when {
                        !tc.isComplete -> "pending"
                        tc.isError -> "error"
                        else -> "success"
                    }
                    sb.append("[工具]${tc.toolName}[/工具]")
                    sb.append("[状态]${status}[/状态]")
                    val resultStr = tc.result?.takeIf { it.isNotBlank() } ?: "无"
                    sb.append("[结果]${resultStr}[/结果]\n")
                }
                sb.append("[工具调用结束]\n")
            }
        }
        return sb.toString()
    }
    
    private suspend fun sendMessageStreaming(
        sessionId: Long,
        content: String,
        aiContext: AIContext
    ) {
        AIRequestService.start(context)
        isStopped = false
        _streamingExpanded.value = emptyMap()
        _streamingMessage.value = StreamingMessage()
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        val userMessage = ChatMsg(
            sessionId = sessionId,
            role = "user",
            content = content
        )
        chatRepository.addMessage(userMessage)
        
        val updatedMessages = chatRepository.getMessagesBySessionOnce(sessionId)
        _uiState.update { it.copy(messages = updatedMessages) }
        
        val historyMessages = chatRepository.getMessagesBySessionOnce(sessionId)
        
        val aiMessages = mutableListOf<AIChatMessage>()
        aiMessages.add(AIChatMessage("system", aiService.buildSystemPrompt(aiContext)))
        historyMessages.forEach { msg ->
            aiMessages.add(AIChatMessage(msg.role, msg.content))
        }
        
        val fullContent = StringBuilder()
        val fullThinking = StringBuilder()
        var isInThinking = false
        var updateVersion = 0L
        
        try {
            aiService.chatStream(aiMessages).collect { result ->
                if (isStopped) {
                    _streamingMessage.value = null
                    _uiState.update { it.copy(isLoading = false) }
                    AIRequestService.stop(context)
                    return@collect
                }
                
                result.fold(
                    onSuccess = { chunk ->
                        val delta = chunk.delta.content
                        
                        if (delta.contains("\u2354") || delta.contains("<tool_call>") || delta.contains("【思考】")) {
                            isInThinking = true
                        }
                        if (delta.contains("\u2355") || delta.contains("⋟") || delta.contains("【/思考】")) {
                            isInThinking = false
                        }
                        
                        if (isInThinking) {
                            val cleanedDelta = delta
                                .replace("\u2354", "")
                                .replace("\u2355", "")
                                .replace("<tool_call>", "")
                                .replace("⋟", "")
                                .replace("【思考】", "")
                                .replace("【/思考】", "")
                            fullThinking.append(cleanedDelta)
                        } else {
                            val cleanedDelta = delta
                                .replace("\u2354", "")
                                .replace("\u2355", "")
                                .replace("<tool_call>", "")
                                .replace("⋟", "")
                                .replace("【思考】", "")
                                .replace("【/思考】", "")
                            fullContent.append(cleanedDelta)
                        }
                        
                        updateVersion++
                        _streamingMessage.value = StreamingMessage(
                            rounds = listOf(StreamingRound(
                                round = 1,
                                thinking = fullThinking.toString()
                            )),
                            content = fullContent.toString(),
                            isComplete = chunk.finishReason != null,
                            updateVersion = updateVersion
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "流式输出错误", error)
                        _streamingMessage.value = null
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "发送失败"
                            )
                        }
                    }
                )
            }
            
            val finalContent = fullContent.toString().trim()
            val finalThinking = fullThinking.toString().trim()
            
            if (finalContent.isNotEmpty() || finalThinking.isNotEmpty()) {
                val messageToSave = if (finalThinking.isNotEmpty()) {
                    "\u2354$finalThinking\u2355\n\n$finalContent"
                } else {
                    finalContent
                }
                
                val assistantMessage = ChatMsg(
                    sessionId = sessionId,
                    role = "assistant",
                    content = messageToSave
                )
                chatRepository.addMessage(assistantMessage)
            }
            
            val finalMessages = chatRepository.getMessagesBySessionOnce(sessionId)
            _streamingMessage.value = null
            _uiState.update { 
                it.copy(
                    messages = finalMessages,
                    isLoading = false,
                    refreshTrigger = it.refreshTrigger + 1
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "流式输出异常", e)
            val finalMessages = chatRepository.getMessagesBySessionOnce(sessionId)
            _streamingMessage.value = null
            _uiState.update { 
                it.copy(
                    messages = finalMessages,
                    isLoading = false,
                    error = e.message ?: "发送失败"
                )
            }
        }
        
        AIRequestService.stop(context)
    }
    
    fun clearMessages() {
        viewModelScope.launch {
            val sessionId = _uiState.value.currentSessionId
            if (sessionId != 0L) {
                chatRepository.deleteMessagesBySession(sessionId)
            }
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private fun formatCurrentTime(): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
    
    data class UndoResult(
        val success: Boolean,
        val message: String
    )
    
    suspend fun canUndoMessage(messageId: Long): Boolean {
        val operations = aiOperationRepository.getByMessageId(messageId)
        return operations.isNotEmpty()
    }
    
    suspend fun getOperationsForMessage(messageId: Long): List<com.bicy.novel.data.local.entity.AIOperationEntity> {
        return aiOperationRepository.getByMessageId(messageId)
    }
    
    fun undoMessage(messageId: Long, onSuccess: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val operations = aiOperationRepository.getByMessageId(messageId)
                if (operations.isEmpty()) {
                    onError("该消息没有可撤销的操作")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                
                val results = mutableListOf<String>()
                val reversedOperations = operations.sortedByDescending { it.timestamp }
                
                for (op in reversedOperations) {
                    val result = executeUndoOperation(op)
                    results.add(result)
                }
                
                aiOperationRepository.deleteByMessageId(messageId)
                
                val successMessage = buildString {
                    append("撤销成功:\n")
                    results.forEach { append("- $it\n") }
                }
                
                val sessionId = _uiState.value.currentSessionId
                if (sessionId != 0L) {
                    val finalMessages = chatRepository.getMessagesBySessionOnce(sessionId)
                    _uiState.update { it.copy(messages = finalMessages, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
                
                onSuccess(successMessage)
            } catch (e: Exception) {
                Log.e(TAG, "撤销失败", e)
                _uiState.update { it.copy(isLoading = false) }
                onError("撤销失败: ${e.message}")
            }
        }
    }
    
    private suspend fun executeUndoOperation(op: com.bicy.novel.data.local.entity.AIOperationEntity): String {
        return when (op.operationType) {
            "CREATE" -> undoCreate(op)
            "UPDATE" -> undoUpdate(op)
            "DELETE" -> undoDelete(op)
            else -> "未知操作类型: ${op.operationType}"
        }
    }
    
    private suspend fun undoCreate(op: com.bicy.novel.data.local.entity.AIOperationEntity): String {
        return when (op.targetType) {
            "chapter" -> {
                chapterRepository.deleteChapter(op.targetId)
                "删除章节「${op.targetName}」"
            }
            "character" -> {
                val character = characterRepository.getCharacterById(op.targetId)
                if (character != null) {
                    characterRepository.deleteCharacter(character)
                    "删除角色「${op.targetName}」"
                } else {
                    "角色已不存在"
                }
            }
            "worldview" -> {
                val worldview = worldviewRepository.getWorldviewById(op.targetId)
                if (worldview != null) {
                    worldviewRepository.deleteWorldview(worldview)
                    "删除世界观「${op.targetName}」"
                } else {
                    "世界观已不存在"
                }
            }
            "note" -> {
                val note = noteRepository.getNoteById(op.targetId)
                if (note != null) {
                    noteRepository.deleteNote(note)
                    "删除笔记「${op.targetName}」"
                } else {
                    "笔记已不存在"
                }
            }
            "timeline" -> {
                val event = timelineRepository.getTimelineById(op.targetId)
                if (event != null) {
                    timelineRepository.deleteTimeline(event)
                    "删除时间线事件「${op.targetName}」"
                } else {
                    "时间线事件已不存在"
                }
            }
            else -> "未知目标类型: ${op.targetType}"
        }
    }
    
    private suspend fun undoUpdate(op: com.bicy.novel.data.local.entity.AIOperationEntity): String {
        if (op.beforeData == null) return "无之前数据，无法恢复"
        
        val beforeJson = JSONObject(op.beforeData)
        
        return when (op.targetType) {
            "chapter" -> {
                val chapter = chapterRepository.getChapterById(op.targetId)
                if (chapter != null) {
                    val restored = chapter.copy(
                        title = beforeJson.optString("title", chapter.title),
                        content = beforeJson.optString("content", chapter.content),
                        wordCount = beforeJson.optInt("wordCount", chapter.wordCount),
                        sortOrder = beforeJson.optInt("sortOrder", chapter.sortOrder)
                    )
                    chapterRepository.updateChapter(restored)
                    "恢复章节「${op.targetName}」"
                } else {
                    "章节已不存在"
                }
            }
            "character" -> {
                val character = characterRepository.getCharacterById(op.targetId)
                if (character != null) {
                    val restored = character.copy(
                        name = beforeJson.optString("name", character.name),
                        description = beforeJson.optString("description", character.description)
                    )
                    characterRepository.updateCharacter(restored)
                    "恢复角色「${op.targetName}」"
                } else {
                    "角色已不存在"
                }
            }
            "worldview" -> {
                val worldview = worldviewRepository.getWorldviewById(op.targetId)
                if (worldview != null) {
                    val restored = worldview.copy(
                        title = beforeJson.optString("title", worldview.title),
                        content = beforeJson.optString("content", worldview.content)
                    )
                    worldviewRepository.updateWorldview(restored)
                    "恢复世界观「${op.targetName}」"
                } else {
                    "世界观已不存在"
                }
            }
            "note" -> {
                val note = noteRepository.getNoteById(op.targetId)
                if (note != null) {
                    val restored = note.copy(
                        title = beforeJson.optString("title", note.title),
                        content = beforeJson.optString("content", note.content)
                    )
                    noteRepository.updateNote(restored)
                    "恢复笔记「${op.targetName}」"
                } else {
                    "笔记已不存在"
                }
            }
            "timeline" -> {
                val event = timelineRepository.getTimelineById(op.targetId)
                if (event != null) {
                    val restored = event.copy(
                        title = beforeJson.optString("title", event.title),
                        description = beforeJson.optString("description", event.description)
                    )
                    timelineRepository.updateTimeline(restored)
                    "恢复时间线事件「${op.targetName}」"
                } else {
                    "时间线事件已不存在"
                }
            }
            else -> "未知目标类型: ${op.targetType}"
        }
    }
    
    private suspend fun undoDelete(op: com.bicy.novel.data.local.entity.AIOperationEntity): String {
        if (op.beforeData == null) return "无之前数据，无法恢复"
        
        val beforeJson = JSONObject(op.beforeData)
        
        return when (op.targetType) {
            "chapter" -> {
                val chapter = Chapter(
                    novelId = currentNovelId,
                    title = beforeJson.optString("title", ""),
                    content = beforeJson.optString("content", ""),
                    wordCount = beforeJson.optInt("wordCount", 0),
                    sortOrder = beforeJson.optInt("sortOrder", 1)
                )
                chapterRepository.createChapter(chapter)
                "重新创建章节「${op.targetName}」"
            }
            "character" -> {
                val character = Character(
                    novelId = currentNovelId,
                    name = beforeJson.optString("name", ""),
                    description = beforeJson.optString("description", "")
                )
                characterRepository.createCharacter(character)
                "重新创建角色「${op.targetName}」"
            }
            "worldview" -> {
                val worldview = Worldview(
                    novelId = currentNovelId,
                    title = beforeJson.optString("title", ""),
                    content = beforeJson.optString("content", "")
                )
                worldviewRepository.createWorldview(worldview)
                "重新创建世界观「${op.targetName}」"
            }
            "note" -> {
                val note = Note(
                    novelId = currentNovelId,
                    title = beforeJson.optString("title", ""),
                    content = beforeJson.optString("content", "")
                )
                noteRepository.createNote(note)
                "重新创建笔记「${op.targetName}」"
            }
            "timeline" -> {
                val event = TimelineEvent(
                    novelId = currentNovelId,
                    title = beforeJson.optString("title", ""),
                    description = beforeJson.optString("description", "")
                )
                timelineRepository.createTimeline(event)
                "重新创建时间线事件「${op.targetName}」"
            }
            else -> "未知目标类型: ${op.targetType}"
        }
    }
}