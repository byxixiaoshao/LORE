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
fun WorldviewEditScreen(
    novelId: Long,
    worldviewId: Long?,
    onBackClick: () -> Unit,
    viewModel: WorldviewEditViewModel = hiltViewModel()
) {
    var initialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(worldviewId, novelId) {
        if (!initialized) {
            viewModel.init(novelId, worldviewId)
            initialized = true
        }
    }
    
    val title by viewModel.title.collectAsState()
    val category by viewModel.category.collectAsState()
    val content by viewModel.content.collectAsState()
    
    val categories = listOf("设定", "势力", "地点", "体系", "其他")
    var expanded by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("世界观 - ${if (worldviewId == null) "新建" else title}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveWorldview(onBackClick) },
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
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = viewModel::setCategory,
                    label = { Text("分类") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = false,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { viewModel.setCategory(cat); expanded = false }
                        )
                    }
                }
            }
            
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
