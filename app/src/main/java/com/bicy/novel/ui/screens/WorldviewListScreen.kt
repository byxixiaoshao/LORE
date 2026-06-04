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
import com.bicy.novel.domain.model.Worldview
import com.bicy.novel.ui.components.EmptyState
import com.bicy.novel.ui.components.WorldviewItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldviewListScreen(
    novelId: Long,
    onBackClick: () -> Unit,
    onWorldviewClick: (Long) -> Unit,
    onAddWorldviewClick: () -> Unit,
    viewModel: WorldviewListViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.setNovelId(novelId)
    }
    
    val worldviews: List<Worldview> by viewModel.worldviews.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<Worldview?>(null) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("世界观") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWorldviewClick) {
                Icon(Icons.Default.Add, contentDescription = "添加设定")
            }
        }
    ) { padding ->
        if (worldviews.isEmpty()) {
            EmptyState(
                message = "还没有世界观设定，点击右下角添加",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 80.dp
                )
            ) {
                items(worldviews, key = { it.id }) { worldview ->
                    WorldviewItem(
                        worldview = worldview,
                        onClick = { onWorldviewClick(worldview.id) }
                    )
                }
            }
        }
    }
    
    showDeleteDialog?.let { worldview ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${worldview.title}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorldview(worldview)
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
