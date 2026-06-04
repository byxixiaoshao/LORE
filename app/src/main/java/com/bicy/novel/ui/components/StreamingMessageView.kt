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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bicy.novel.ui.screens.StreamingMessage
import com.bicy.novel.ui.screens.ToolCallEntry

@Composable
fun StreamingMessageView(
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
                    ToolCallsSection(toolCalls = round.toolCalls)
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
private fun ToolCallsSection(toolCalls: List<ToolCallEntry>) {
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
                    ToolCallItemRow(toolCall)
                }
            }
        }
    }
}

@Composable
private fun ToolCallItemRow(toolCall: ToolCallEntry) {
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
