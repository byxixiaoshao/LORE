package com.bicy.novel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bicy.novel.domain.model.ContentHistory
import com.bicy.novel.util.WordCounter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ContentHistoryScreen(
    targetId: Long,
    targetType: String = "chapter",
    onBackClick: () -> Unit,
    onHistoryRestored: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContentHistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(targetId, targetType) {
        viewModel.loadHistory(targetId, targetType)
    }
    
    val state by viewModel.state.collectAsState()
    var showRestoreSuccess by remember { mutableStateOf(false) }
    var showCompareDialog by remember { mutableStateOf(false) }
    var compareHistory by remember { mutableStateOf<ContentHistory?>(null) }
    
    val typeLabel = when (targetType) {
        "chapter" -> "章节"
        "character" -> "角色"
        "worldview" -> "世界观"
        "note" -> "笔记"
        "timeline" -> "时间线"
        else -> "内容"
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top Bar
        TopAppBar(
            title = { Text("$typeLabel 版本历史") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.deleteAllHistory(targetId, targetType) }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "清空历史")
                }
            }
        )
        
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.histories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无历史记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.histories) { history ->
                    HistoryItem(
                        history = history,
                        onClick = { viewModel.selectHistory(history) },
                        onRestore = {
                            viewModel.selectHistory(history)
                            viewModel.showRestoreDialog()
                        },
                        onDelete = {
                            viewModel.selectHistory(history)
                            viewModel.showDeleteDialog()
                        },
                        onCompare = {
                            compareHistory = history
                            showCompareDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Restore Dialog
    if (state.showRestoreDialog && state.selectedHistory != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRestoreDialog() },
            title = { Text("恢复版本") },
            text = { Text("确定要恢复到这个版本吗？当前内容将被替换。恢复后将返回编辑页面。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.restoreHistory {
                        onHistoryRestored()
                        onBackClick() // 恢复后返回编辑页面
                    }
                }) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRestoreDialog() }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Delete Dialog
    if (state.showDeleteDialog && state.selectedHistory != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("删除版本") },
            text = { Text("确定要删除这个历史版本吗？") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteHistory() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Compare Dialog
    if (showCompareDialog && compareHistory != null) {
        CompareDialog(
            oldContent = compareHistory!!.content,
            newContent = state.currentContent,
            historyTitle = compareHistory!!.title,
            onDismiss = { 
                showCompareDialog = false
                compareHistory = null
            }
        )
    }
}

@Composable
private fun HistoryItem(
    history: ContentHistory,
    onClick: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onCompare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = history.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTime(history.savedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = onCompare) {
                        Icon(
                            Icons.Default.Compare,
                            contentDescription = "对比",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onRestore) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "恢复",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "字数：${WordCounter.formatWordCount(history.wordCount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                history.note?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 内容预览
            if (history.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = history.content.take(100) + if (history.content.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    } catch (e: Exception) {
        timestamp.toString()
    }
}

@Composable
private fun CompareDialog(
    oldContent: String,
    newContent: String,
    historyTitle: String,
    onDismiss: () -> Unit
) {
    val diffLines = remember(oldContent, newContent) {
        com.bicy.novel.util.DiffUtils.diff(oldContent, newContent)
    }
    val diffStats = remember(diffLines) {
        com.bicy.novel.util.DiffUtils.getDiffStats(diffLines)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("差异对比 - $historyTitle") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 统计信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "变更统计",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("新增: ${diffStats.addedLines}", color = MaterialTheme.colorScheme.primary)
                            Text("删除: ${diffStats.removedLines}", color = MaterialTheme.colorScheme.error)
                            Text("修改: ${diffStats.modifiedLines}", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 差异内容
                diffLines.forEach { line ->
                    val backgroundColor = when (line.type) {
                        com.bicy.novel.util.DiffType.ADDED -> 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        com.bicy.novel.util.DiffType.REMOVED -> 
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        com.bicy.novel.util.DiffType.MODIFIED -> 
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        com.bicy.novel.util.DiffType.UNCHANGED -> 
                            MaterialTheme.colorScheme.surface
                    }
                    
                    val textColor = when (line.type) {
                        com.bicy.novel.util.DiffType.ADDED -> 
                            MaterialTheme.colorScheme.primary
                        com.bicy.novel.util.DiffType.REMOVED -> 
                            MaterialTheme.colorScheme.error
                        com.bicy.novel.util.DiffType.MODIFIED -> 
                            MaterialTheme.colorScheme.secondary
                        com.bicy.novel.util.DiffType.UNCHANGED -> 
                            MaterialTheme.colorScheme.onSurface
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${line.lineNumber}",
                            modifier = Modifier.width(40.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = line.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 浮窗形式的历史记录查看器
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ContentHistoryDialog(
    targetId: Long,
    targetType: String = "chapter",
    onDismiss: () -> Unit,
    onRestored: () -> Unit,
    viewModel: ContentHistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(targetId, targetType) {
        viewModel.loadHistory(targetId, targetType)
    }
    
    val state by viewModel.state.collectAsState()
    var showCompareDialog by remember { mutableStateOf(false) }
    var compareHistory by remember { mutableStateOf<ContentHistory?>(null) }
    
    val typeLabel = when (targetType) {
        "chapter" -> "章节"
        "character" -> "角色"
        "worldview" -> "世界观"
        "note" -> "笔记"
        "timeline" -> "时间线"
        else -> "内容"
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$typeLabel 版本历史") },
        text = {
            Box(modifier = Modifier.height(400.dp)) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.histories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无历史记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.histories) { history ->
                            HistoryItem(
                                history = history,
                                onClick = { viewModel.selectHistory(history) },
                                onRestore = {
                                    viewModel.selectHistory(history)
                                    viewModel.showRestoreDialog()
                                },
                                onDelete = {
                                    viewModel.selectHistory(history)
                                    viewModel.showDeleteDialog()
                                },
                                onCompare = {
                                    compareHistory = history
                                    showCompareDialog = true
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { viewModel.deleteAllHistory(targetId, targetType) }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    )
    
    // Restore Dialog
    if (state.showRestoreDialog && state.selectedHistory != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRestoreDialog() },
            title = { Text("恢复版本") },
            text = { Text("确定要恢复到这个版本吗？当前内容将被替换。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.restoreHistory {
                        onRestored()
                    }
                }) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRestoreDialog() }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Delete Dialog
    if (state.showDeleteDialog && state.selectedHistory != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("删除版本") },
            text = { Text("确定要删除这个历史版本吗？") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteHistory() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Compare Dialog
    if (showCompareDialog && compareHistory != null) {
        CompareDialog(
            oldContent = compareHistory!!.content,
            newContent = state.currentContent,
            historyTitle = compareHistory!!.title,
            onDismiss = { 
                showCompareDialog = false
                compareHistory = null
            }
        )
    }
}
