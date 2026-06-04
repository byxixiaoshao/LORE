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
import com.bicy.novel.domain.model.Character
import com.bicy.novel.domain.model.Worldview
import com.bicy.novel.ui.components.CharacterItem
import com.bicy.novel.ui.components.EmptyState
import com.bicy.novel.ui.components.WorldviewItem
import com.bicy.novel.ui.components.BatchOperationSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiScreen(
    novelId: Long,
    onCharacterClick: (Long) -> Unit,
    onWorldviewClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WikiViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.setNovelId(novelId)
    }
    
    val characters: List<Character> by viewModel.characters.collectAsState()
    val worldviews: List<Worldview> by viewModel.worldviews.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    
    // 角色批量操作状态
    var showCharacterBatchOperation by remember { mutableStateOf(false) }
    var selectedCharacterIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showCharacterDeleteConfirm by remember { mutableStateOf(false) }
    
    // 世界观批量操作状态
    var showWorldviewBatchOperation by remember { mutableStateOf(false) }
    var selectedWorldviewIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showWorldviewDeleteConfirm by remember { mutableStateOf(false) }
    
    // 角色批量操作Sheet
    if (showCharacterBatchOperation) {
        ModalBottomSheet(
            onDismissRequest = { showCharacterBatchOperation = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            BatchOperationSheet(
                items = characters,
                itemTitle = { it.name },
                itemSubtitle = { "${it.description.length}字" },
                selectedIds = selectedCharacterIds,
                onSelectionChange = { selectedCharacterIds = it },
                onConfirm = { ids ->
                    selectedCharacterIds = ids.toSet()
                    showCharacterDeleteConfirm = true
                },
                onDismiss = { 
                    showCharacterBatchOperation = false
                    selectedCharacterIds = emptySet()
                },
                title = "批量删除角色",
                confirmButtonText = "删除",
                itemId = { it.id }
            )
        }
    }
    
    // 角色删除确认对话框
    if (showCharacterDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showCharacterDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedCharacterIds.size} 个角色吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCharacters(selectedCharacterIds.toList())
                        showCharacterDeleteConfirm = false
                        showCharacterBatchOperation = false
                        selectedCharacterIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCharacterDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 世界观批量操作Sheet
    if (showWorldviewBatchOperation) {
        ModalBottomSheet(
            onDismissRequest = { showWorldviewBatchOperation = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            BatchOperationSheet(
                items = worldviews,
                itemTitle = { it.title },
                itemSubtitle = { "${it.content.length}字" },
                selectedIds = selectedWorldviewIds,
                onSelectionChange = { selectedWorldviewIds = it },
                onConfirm = { ids ->
                    selectedWorldviewIds = ids.toSet()
                    showWorldviewDeleteConfirm = true
                },
                onDismiss = { 
                    showWorldviewBatchOperation = false
                    selectedWorldviewIds = emptySet()
                },
                title = "批量删除世界观",
                confirmButtonText = "删除",
                itemId = { it.id }
            )
        }
    }
    
    // 世界观删除确认对话框
    if (showWorldviewDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showWorldviewDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selectedWorldviewIds.size} 条世界观吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorldviews(selectedWorldviewIds.toList())
                        showWorldviewDeleteConfirm = false
                        showWorldviewBatchOperation = false
                        selectedWorldviewIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorldviewDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("角色") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("世界观") }
            )
        }
        
        when (selectedTab) {
            0 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "共 ${characters.size} 个角色",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        // 批量操作按钮
                        if (characters.isNotEmpty()) {
                            IconButton(onClick = { showCharacterBatchOperation = true }) {
                                Icon(Icons.Default.Checklist, contentDescription = "批量操作")
                            }
                        }
                        IconButton(onClick = { viewModel.createCharacter() }) {
                            Icon(Icons.Default.Add, contentDescription = "添加角色")
                        }
                    }
                }
                
                if (characters.isEmpty()) {
                    EmptyState(
                        message = "还没有角色\n点击右上角 + 添加",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(characters, key = { it.id }) { character ->
                            CharacterItem(
                                character = character,
                                onClick = { onCharacterClick(character.id) }
                            )
                        }
                    }
                }
            }
            1 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "共 ${worldviews.size} 条设定",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        // 批量操作按钮
                        if (worldviews.isNotEmpty()) {
                            IconButton(onClick = { showWorldviewBatchOperation = true }) {
                                Icon(Icons.Default.Checklist, contentDescription = "批量操作")
                            }
                        }
                        IconButton(onClick = { viewModel.createWorldview() }) {
                            Icon(Icons.Default.Add, contentDescription = "添加设定")
                        }
                    }
                }
                
                if (worldviews.isEmpty()) {
                    EmptyState(
                        message = "还没有世界观设定\n点击右上角 + 添加",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
        }
    }
}
