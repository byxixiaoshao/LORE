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
import com.bicy.novel.domain.model.Character
import com.bicy.novel.ui.components.CharacterItem
import com.bicy.novel.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    novelId: Long,
    onBackClick: () -> Unit,
    onCharacterClick: (Long) -> Unit,
    onAddCharacterClick: () -> Unit,
    viewModel: CharacterListViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.setNovelId(novelId)
    }
    
    val characters: List<Character> by viewModel.characters.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<Character?>(null) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("角色管理") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCharacterClick) {
                Icon(Icons.Default.Add, contentDescription = "添加角色")
            }
        }
    ) { padding ->
        if (characters.isEmpty()) {
            EmptyState(
                message = "还没有角色，点击右下角添加",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 80.dp
                )
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
    
    showDeleteDialog?.let { character ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除角色「${character.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCharacter(character)
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
