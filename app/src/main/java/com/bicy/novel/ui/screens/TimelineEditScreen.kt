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
fun TimelineEditScreen(
    novelId: Long,
    eventId: Long?,
    onBackClick: () -> Unit,
    viewModel: TimelineEditViewModel = hiltViewModel()
) {
    var initialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(eventId, novelId) {
        if (!initialized) {
            viewModel.init(novelId, eventId)
            initialized = true
        }
    }
    
    val title by viewModel.title.collectAsState()
    val eventDate by viewModel.eventDate.collectAsState()
    val description by viewModel.description.collectAsState()
    val isKeyEvent by viewModel.isKeyEvent.collectAsState()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("时间线 - ${if (eventId == null) "新建" else title}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveEvent(onBackClick) },
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
                label = { Text("事件标题 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = eventDate,
                onValueChange = viewModel::setEventDate,
                label = { Text("事件时间") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("例如：第一章、2024年春") }
            )
            
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("关键事件")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isKeyEvent,
                    onCheckedChange = viewModel::setKeyEvent
                )
            }
            
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::setDescription,
                label = { Text("事件描述") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                maxLines = 8
            )
        }
    }
}
