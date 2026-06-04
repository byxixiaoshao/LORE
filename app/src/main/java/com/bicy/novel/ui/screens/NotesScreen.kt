package com.bicy.novel.ui.screens

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
import com.bicy.novel.domain.model.Note
import com.bicy.novel.ui.components.EmptyState
import com.bicy.novel.ui.components.NoteItem
import com.bicy.novel.ui.components.BatchOperationSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    novelId: Long,
    onNoteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.setNovelId(novelId)
    }
    
    val notes: List<Note> by viewModel.notes.collectAsState()
    
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
                items = notes,
                itemTitle = { it.title },
                itemSubtitle = { "${it.content.length}字" },
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
                title = "批量删除笔记",
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
            text = { Text("确定要删除选中的 ${selectedIds.size} 条笔记吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotes(selectedIds.toList())
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
                text = "共 ${notes.size} 条笔记",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                // 批量操作按钮
                if (notes.isNotEmpty()) {
                    IconButton(onClick = { showBatchOperation = true }) {
                        Icon(Icons.Default.Checklist, contentDescription = "批量操作")
                    }
                }
                IconButton(onClick = { viewModel.createNote() }) {
                    Icon(Icons.Default.Add, contentDescription = "添加笔记")
                }
            }
        }
        
        if (notes.isEmpty()) {
            EmptyState(
                message = "还没有笔记\n点击右上角 + 添加",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteItem(
                        note = note,
                        onClick = { onNoteClick(note.id) }
                    )
                }
            }
        }
    }
}
