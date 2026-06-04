package com.bicy.novel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bicy.novel.domain.model.Chapter
import com.bicy.novel.ui.components.ChapterItem
import com.bicy.novel.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    novelId: Long,
    onBackClick: () -> Unit,
    onChapterClick: (Long) -> Unit,
    onAddChapterClick: () -> Unit,
    viewModel: ChapterListViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.setNovelId(novelId)
    }
    
    val chapters: List<Chapter> by viewModel.chapters.collectAsState()
    val novelTitle by viewModel.novelTitle.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(novelTitle.ifEmpty { "章节管理" }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddChapterClick) {
                Icon(Icons.Default.Add, contentDescription = "添加章节")
            }
        }
    ) { padding ->
        if (chapters.isEmpty()) {
            EmptyState(
                message = "还没有章节，点击右下角添加",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 80.dp
                )
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
    
    showDeleteDialog?.let { chapterId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个章节吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChapter(chapterId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}
