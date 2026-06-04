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
import com.bicy.novel.domain.model.Chapter
import com.bicy.novel.ui.components.ChapterItem
import com.bicy.novel.ui.components.EmptyState
import com.bicy.novel.ui.components.BatchOperationSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentScreen(
    novelId: Long,
    onChapterClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContentViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.setNovelId(novelId)
    }
    
    val chapters: List<Chapter> by viewModel.chapters.collectAsState()
    val navigateToEdit by viewModel.navigateToEdit.collectAsState()
    
    LaunchedEffect(navigateToEdit) {
        navigateToEdit?.let { (novelId, chapterId) ->
            onChapterClick(chapterId!!)
            viewModel.clearNavigation()
        }
    }
    
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
                items = chapters,
                itemTitle = { it.title },
                itemSubtitle = { "${it.wordCount}字" },
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
                title = "批量删除章节",
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
            text = { Text("确定要删除选中的 ${selectedIds.size} 个章节吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChapters(selectedIds.toList())
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
                text = "共 ${chapters.size} 章",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                // 批量操作按钮
                if (chapters.isNotEmpty()) {
                    IconButton(onClick = { showBatchOperation = true }) {
                        Icon(Icons.Default.Checklist, contentDescription = "批量操作")
                    }
                }
                IconButton(onClick = { viewModel.createChapter() }) {
                    Icon(Icons.Default.Add, contentDescription = "添加章节")
                }
            }
        }
        
        if (chapters.isEmpty()) {
            EmptyState(
                message = "还没有章节\n点击右上角 + 添加",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(chapters, key = { it.id }) { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        onClick = { onChapterClick(chapter.id) }
                    )
                }
            }
        }
    }
}
