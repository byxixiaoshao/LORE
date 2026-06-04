package com.bicy.novel.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bicy.novel.ui.screens.StreamingMessage
import com.bicy.novel.ui.screens.ToolCallEntry
import dev.jeziellago.compose.markdowntext.MarkdownText

enum class SegmentType {
    THINKING, TOOL_CALL, CONTENT
}

data class MessageSegment(
    val type: SegmentType,
    val content: String,
    val toolCalls: List<ToolCallDisplayInfo>? = null,
    val round: Int = 0
)

data class ToolCallDisplayInfo(
    val toolName: String,
    val arguments: String,
    val result: String? = null,
    val isError: Boolean = false,
    val isComplete: Boolean = false
)

data class OperationInfo(
    val operationType: String,
    val targetType: String,
    val targetName: String
)

data class ParsedAIMessage(
    val segments: List<MessageSegment>,
    val operations: List<OperationInfo> = emptyList()
)

fun parseAIMessageContent(content: String): ParsedAIMessage {
    val segments = mutableListOf<MessageSegment>()
    val thinkingStartTag = "\u2354"
    val thinkingEndTag = "\u2355"
    
    var remaining = content
    var round = 0
    
    while (remaining.isNotEmpty()) {
        var thinkingStart = remaining.indexOf(thinkingStartTag)
        var thinkingEnd = remaining.indexOf(thinkingEndTag)
        var startTagLen = 1
        var endTagLen = 1
        
        if (thinkingStart == -1) {
            thinkingStart = remaining.indexOf("<tool_call>")
            thinkingEnd = remaining.indexOf("⋟")
            startTagLen = 2
            endTagLen = 2
        }
        
        if (thinkingStart == -1) {
            thinkingStart = remaining.indexOf("【思考】")
            thinkingEnd = remaining.indexOf("【/思考】")
            startTagLen = 4
            endTagLen = 4
        }
        
        if (thinkingStart != -1 && thinkingEnd != -1 && thinkingEnd > thinkingStart) {
            if (thinkingStart > 0) {
                val beforeThinking = remaining.substring(0, thinkingStart).trim()
                if (beforeThinking.isNotEmpty()) {
                    val (contentBefore, toolCallPart) = extractContentBeforeToolCall(beforeThinking)
                    
                    if (contentBefore.isNotBlank()) {
                        segments.add(MessageSegment(
                            type = SegmentType.CONTENT,
                            content = contentBefore,
                            round = round
                        ))
                    }
                    
                    if (toolCallPart.isNotEmpty()) {
                        val toolCallInfo = parseToolCallSection(toolCallPart)
                        if (toolCallInfo != null) {
                            segments.add(MessageSegment(
                                type = SegmentType.TOOL_CALL,
                                content = "",
                                toolCalls = toolCallInfo,
                                round = round
                            ))
                        }
                    }
                }
            }
            
            val thinkingContent = remaining.substring(thinkingStart + startTagLen, thinkingEnd).trim()
            if (thinkingContent.isNotEmpty()) {
                round++
                segments.add(MessageSegment(
                    type = SegmentType.THINKING,
                    content = thinkingContent,
                    round = round
                ))
            }
            
            remaining = remaining.substring(thinkingEnd + endTagLen).trim()
        } else if (thinkingStart != -1) {
            if (thinkingStart > 0) {
                val beforeThinking = remaining.substring(0, thinkingStart).trim()
                if (beforeThinking.isNotEmpty()) {
                    val (contentBefore, toolCallPart) = extractContentBeforeToolCall(beforeThinking)
                    
                    if (contentBefore.isNotBlank()) {
                        segments.add(MessageSegment(
                            type = SegmentType.CONTENT,
                            content = contentBefore,
                            round = round
                        ))
                    }
                    
                    if (toolCallPart.isNotEmpty()) {
                        val toolCallInfo = parseToolCallSection(toolCallPart)
                        if (toolCallInfo != null) {
                            segments.add(MessageSegment(
                                type = SegmentType.TOOL_CALL,
                                content = "",
                                toolCalls = toolCallInfo,
                                round = round
                            ))
                        }
                    }
                }
            }
            
            val thinkingContent = remaining.substring(thinkingStart + startTagLen).trim()
            if (thinkingContent.isNotEmpty()) {
                round++
                segments.add(MessageSegment(
                    type = SegmentType.THINKING,
                    content = thinkingContent,
                    round = round
                ))
            }
            break
        } else {
            if (remaining.isNotEmpty()) {
                val (contentBefore, toolCallPart) = extractContentBeforeToolCall(remaining)
                
                if (contentBefore.isNotBlank()) {
                    segments.add(MessageSegment(
                        type = SegmentType.CONTENT,
                        content = contentBefore,
                        round = round
                    ))
                }
                
                if (toolCallPart.isNotEmpty()) {
                    val toolCallInfo = parseToolCallSection(toolCallPart)
                    if (toolCallInfo != null) {
                        segments.add(MessageSegment(
                            type = SegmentType.TOOL_CALL,
                            content = "",
                            toolCalls = toolCallInfo,
                            round = round
                        ))
                    }
                    
                    val toolCallEndTag = "[工具调用结束]"
                    val endPos = remaining.indexOf(toolCallEndTag)
                    if (endPos != -1) {
                        remaining = remaining.substring(endPos + toolCallEndTag.length).trim()
                        continue
                    }
                }
            }
            break
        }
    }
    
    return ParsedAIMessage(segments = segments)
}

private fun parseToolCallSection(content: String): List<ToolCallDisplayInfo>? {
    val toolCallStart = content.indexOf("[工具调用开始]")
    val toolCallEnd = content.indexOf("[工具调用结束]", toolCallStart)
    
    android.util.Log.d("AIMessageRenderer", "parseToolCallSection: start=$toolCallStart, end=$toolCallEnd")
    
    if (toolCallStart != -1) {
        val toolCallContent = if (toolCallEnd != -1 && toolCallEnd > toolCallStart) {
            content.substring(toolCallStart + 7, toolCallEnd)
        } else {
            content.substring(toolCallStart + 7)
        }
        val toolCalls = mutableListOf<ToolCallDisplayInfo>()
        
        val toolRegex = """\[工具\](.*?)\[/工具\]\[状态\](.*?)\[/状态\]\[结果\]([\s\S]*?)\[/结果\]""".toRegex()
        toolRegex.findAll(toolCallContent).forEach { match ->
            val toolName = match.groupValues[1]
            val status = match.groupValues[2]
            val result = match.groupValues[3].trim()
            val displayResult = if (result.length > 100) result.take(100) + "..." else result
            
            toolCalls.add(ToolCallDisplayInfo(
                toolName = toolName,
                arguments = "",
                result = displayResult.ifBlank { null },
                isComplete = status != "pending",
                isError = status == "error"
            ))
        }
        
        android.util.Log.d("AIMessageRenderer", "解析到 ${toolCalls.size} 个工具调用")
        
        if (toolCalls.isNotEmpty()) {
            return toolCalls
        }
    }
    
    val oldToolCallStart = content.indexOf("[工具调用:")
    val oldToolCallEnd = if (oldToolCallStart != -1) content.indexOf("]", oldToolCallStart) else -1
    
    if (oldToolCallStart != -1 && oldToolCallEnd != -1 && oldToolCallEnd > oldToolCallStart) {
        val toolCallStr = content.substring(oldToolCallStart + 5, oldToolCallEnd).trim()
        val regex = """(\d+)成功[,\s]*(\d+)失败""".toRegex()
        val match = regex.find(toolCallStr)
        if (match != null) {
            val successCount = match.groupValues[1].toIntOrNull() ?: 0
            val failCount = match.groupValues[2].toIntOrNull() ?: 0
            return listOf(ToolCallDisplayInfo(
                toolName = "工具调用",
                arguments = "",
                result = "${successCount}个成功, ${failCount}个失败",
                isComplete = true,
                isError = failCount > 0 && successCount == 0
            ))
        }
        
        val simpleRegex = """(\d+)成功""".toRegex()
        val simpleMatch = simpleRegex.find(toolCallStr)
        if (simpleMatch != null) {
            val successCount = simpleMatch.groupValues[1].toIntOrNull() ?: 0
            return listOf(ToolCallDisplayInfo(
                toolName = "工具调用",
                arguments = "",
                result = "${successCount}个成功",
                isComplete = true
            ))
        }
    }
    return null
}

fun extractContentBeforeToolCall(content: String): Pair<String, String> {
    val newToolCallStart = content.indexOf("[工具调用开始]")
    if (newToolCallStart != -1) {
        val toolCallEnd = content.indexOf("[工具调用结束]", newToolCallStart)
        val before = content.substring(0, newToolCallStart).trim()
        val after = if (toolCallEnd != -1) {
            content.substring(newToolCallStart, toolCallEnd + 7)
        } else {
            content.substring(newToolCallStart)
        }
        return Pair(before, after)
    }
    
    val oldToolCallStart = content.indexOf("[工具调用:")
    if (oldToolCallStart != -1) {
        val before = content.substring(0, oldToolCallStart).trim()
        val after = content.substring(oldToolCallStart)
        return Pair(before, after)
    }
    return Pair(content, "")
}

@Composable
fun AIMessageContent(
    content: String,
    isUser: Boolean,
    messageId: Long = 0,
    operations: List<OperationInfo> = emptyList(),
    canUndo: Boolean = false,
    onUndo: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val parsed = remember(content) { parseAIMessageContent(content) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (isUser) "你" else "AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
                if (!isUser) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = contentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isUser) {
                Text(
                    content,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            } else {
                parsed.segments.forEachIndexed { index, segment ->
                    when (segment.type) {
                        SegmentType.THINKING -> {
                            key(segment.round) {
                                ThinkingSection(
                                    thinking = segment.content,
                                    round = segment.round
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        SegmentType.TOOL_CALL -> {
                            if (!segment.toolCalls.isNullOrEmpty()) {
                                ToolCallsSection(toolCalls = segment.toolCalls)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                        SegmentType.CONTENT -> {
                            if (segment.content.isNotEmpty()) {
                                MarkdownContent(
                                    content = segment.content,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
                
                if (operations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OperationsSection(
                        operations = operations,
                        canUndo = canUndo,
                        onUndo = { onUndo?.invoke(messageId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingSection(
    thinking: String,
    round: Int = 0
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (round > 0) "思考过程 (第${round}轮)" else "思考过程",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Text(
                thinking,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MarkdownContent(
    content: String,
    color: androidx.compose.ui.graphics.Color
) {
    if (content.contains("```") || content.contains("**") || content.contains("#") || content.contains("- ") || content.contains("[")) {
        MarkdownText(
            markdown = content,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Text(
            content,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
fun StreamingMessageContent(
    streamingMessage: StreamingMessage
) {
    val containerColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("AI", style = MaterialTheme.typography.labelSmall, color = contentColor)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = contentColor
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            streamingMessage.rounds.forEach { round ->
                if (round.thinking.isNotEmpty()) {
                    key(round.round) {
                        ThinkingSection(
                            thinking = round.thinking,
                            round = round.round
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                if (round.toolCalls.isNotEmpty()) {
                    StreamingToolCallsSection(toolCalls = round.toolCalls)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            
            if (streamingMessage.content.isNotEmpty()) {
                MarkdownContent(
                    content = streamingMessage.content,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun StreamingToolCallsSection(toolCalls: List<com.bicy.novel.ui.screens.ToolCallEntry>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "工具调用",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        toolCalls.forEach { toolCall ->
            StreamingToolCallItemRow(toolCall)
        }
    }
}

@Composable
private fun StreamingToolCallItemRow(toolCall: com.bicy.novel.ui.screens.ToolCallEntry) {
    val toolDisplayName = when (toolCall.toolName) {
        "create" -> "创建"
        "edit" -> "编辑"
        "delete" -> "删除"
        "view" -> "查看"
        "search" -> "搜索"
        "move_chapter" -> "移动章节"
        "merge_chapters" -> "合并章节"
        "split_chapter" -> "拆分章节"
        "list_characters" -> "列出角色"
        "list_worldviews" -> "列出世界观"
        "list_notes" -> "列出笔记"
        "list_timeline" -> "列出时间线"
        "count_words" -> "统计字数"
        "check_consistency" -> "检查一致性"
        else -> toolCall.toolName
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "• $toolDisplayName",
            style = MaterialTheme.typography.labelSmall,
            color = when {
                toolCall.isError -> MaterialTheme.colorScheme.error
                toolCall.isComplete -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onTertiaryContainer
            }
        )
        
        if (!toolCall.isComplete) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.dp
            )
        } else if (toolCall.result != null) {
            Text(
                "→ ${toolCall.result.take(40)}${if (toolCall.result.length > 40) "..." else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = if (toolCall.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OperationsSection(
    operations: List<OperationInfo>,
    canUndo: Boolean,
    onUndo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "AI操作",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (canUndo) {
                TextButton(
                    onClick = onUndo,
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "撤销",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "撤销",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        operations.forEach { op ->
            val opDisplayName = when (op.operationType) {
                "CREATE" -> "创建"
                "UPDATE" -> "编辑"
                "DELETE" -> "删除"
                else -> op.operationType
            }
            
            val targetDisplayName = when (op.targetType) {
                "chapter" -> "章节"
                "character" -> "角色"
                "worldview" -> "世界观"
                "note" -> "笔记"
                "timeline" -> "时间线"
                else -> op.targetType
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val opColor = when (op.operationType) {
                    "CREATE" -> MaterialTheme.colorScheme.primary
                    "UPDATE" -> MaterialTheme.colorScheme.tertiary
                    "DELETE" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                Text(
                    "• $opDisplayName $targetDisplayName「${op.targetName}」",
                    style = MaterialTheme.typography.labelSmall,
                    color = opColor
                )
            }
        }
    }
}

@Composable
private fun ToolCallsSection(toolCalls: List<ToolCallDisplayInfo>) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "工具调用",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                toolCalls.forEach { toolCall ->
                    val toolDisplayName = when (toolCall.toolName) {
                        "create" -> "创建"
                        "edit" -> "编辑"
                        "delete" -> "删除"
                        "view" -> "查看"
                        "search" -> "搜索"
                        "move_chapter" -> "移动章节"
                        "merge_chapters" -> "合并章节"
                        "split_chapter" -> "拆分章节"
                        "list_characters" -> "列出角色"
                        "list_worldviews" -> "列出世界观"
                        "list_notes" -> "列出笔记"
                        "list_timeline" -> "列出时间线"
                        "count_words" -> "统计字数"
                        "check_consistency" -> "检查一致性"
                        else -> toolCall.toolName
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusIcon = when {
                            !toolCall.isComplete -> "⏳"
                            toolCall.isError -> "❌"
                            else -> "✓"
                        }
                        
                        val color = when {
                            !toolCall.isComplete -> MaterialTheme.colorScheme.onTertiaryContainer
                            toolCall.isError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                        
                        Text(
                            "• $statusIcon $toolDisplayName",
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                        
                        if (toolCall.result != null && toolCall.isComplete) {
                            Text(
                                "→ ${toolCall.result}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (toolCall.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
