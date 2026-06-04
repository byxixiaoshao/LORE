package com.bicy.novel.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bicy.novel.domain.model.TimelineEvent
import com.bicy.novel.ui.components.EmptyState
import com.bicy.novel.ui.components.BatchOperationSheet
import com.bicy.novel.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    novelId: Long,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.setNovelId(novelId)
    }
    
    val events: List<TimelineEvent> by viewModel.events.collectAsState()
    
    var showBatchOperation by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // 批量操作Sheet
    if (showBatchOperation) {
        ModalBottomSheet(
            onDismissRequest = { showBatchOperation = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            BatchOperationSheet(
                items = events,
                itemTitle = { it.title },
                itemSubtitle = { if (it.eventDate.isNotEmpty()) it.eventDate else "无日期" },
                selectedIds = selectedIds,
                onSelectionChange = { selectedIds = it },
                onConfirm = { ids ->
                    selectedIds = ids.toSet()
                    showDeleteConfirm = true
                },
                onDismiss = { 
                    showBatchOperation = false
                    selectedIds = emptySet()
                },
                title = "批量删除时间线事件",
                confirmButtonText = "删除",
                itemId = { it.id }
            )
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 个时间线事件吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvents(selectedIds.toList())
                        showDeleteConfirm = false
                        showBatchOperation = false
                        selectedIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "共 ${events.size} 个事件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                // 批量操作按钮
                if (events.isNotEmpty()) {
                    IconButton(onClick = { showBatchOperation = true }) {
                        Icon(Icons.Default.Checklist, contentDescription = "批量操作")
                    }
                }
                IconButton(onClick = { viewModel.createEvent() }) {
                    Icon(Icons.Default.Add, contentDescription = "添加事件")
                }
            }
        }
        
        if (events.isEmpty()) {
            EmptyState(
                message = "还没有时间线事件\n点击右上角 + 添加",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    TimelineEventItem(
                        event = event,
                        onClick = { onEventClick(event.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEventItem(
    event: TimelineEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (event.isKeyEvent) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "关键",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (event.eventDate.isNotEmpty()) {
                    Text(
                        text = event.eventDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (event.description.isNotEmpty()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
