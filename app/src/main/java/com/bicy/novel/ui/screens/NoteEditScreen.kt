package com.bicy.novel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    novelId: Long,
    noteId: Long?,
    onBackClick: () -> Unit,
    viewModel: NoteEditViewModel = hiltViewModel()
) {
    var initialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(noteId, novelId) {
        if (!initialized) {
            viewModel.init(novelId, noteId)
            initialized = true
        }
    }
    
    val title by viewModel.title.collectAsState()
    val category by viewModel.category.collectAsState()
    val content by viewModel.content.collectAsState()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("灵感笔记 - ${if (noteId == null) "新建" else title}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveNote(onBackClick) },
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::setTitle,
                label = { Text("标题 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = category,
                onValueChange = viewModel::setCategory,
                label = { Text("分类") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = content,
                onValueChange = viewModel::setContent,
                label = { Text("内容") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                maxLines = 15
            )
        }
    }
}
