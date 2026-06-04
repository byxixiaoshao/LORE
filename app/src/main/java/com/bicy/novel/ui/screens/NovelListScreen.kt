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
import com.bicy.novel.ui.components.EmptyState
import com.bicy.novel.ui.components.NovelCard
import com.bicy.novel.ui.components.AddProjectDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelListScreen(
    onNovelClick: (Long) -> Unit,
    onAddNovelClick: () -> Unit,
    onImportNovelClick: () -> Unit,
    viewModel: NovelListViewModel = hiltViewModel()
) {
    val novels by viewModel.novels.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    
    var showSearch by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("搜索小说...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    } else {
                        Text("项目列表")
                    }
                },
                actions = {
                    if (showSearch) {
                        IconButton(onClick = { 
                            showSearch = false
                            viewModel.onSearchQueryChange("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭搜索")
                        }
                    } else {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "添加项目")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val displayNovels = if (isSearching) searchResults else novels
        
        if (displayNovels.isEmpty()) {
            EmptyState(
                message = if (isSearching && searchQuery.isNotBlank()) "未找到匹配的小说" else "还没有项目\n点击右上角 + 创建新项目",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayNovels, key = { it.id }) { novel ->
                    NovelCard(
                        novel = novel,
                        onClick = { onNovelClick(novel.id) }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onCreateNew = onAddNovelClick,
            onImport = onImportNovelClick
        )
    }
    
    showDeleteDialog?.let { novelId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除这本小说吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNovel(novelId)
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
