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
fun CharacterEditScreen(
    novelId: Long,
    characterId: Long?,
    onBackClick: () -> Unit,
    viewModel: CharacterEditViewModel = hiltViewModel()
) {
    var initialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(characterId, novelId) {
        if (!initialized) {
            viewModel.init(novelId, characterId)
            initialized = true
        }
    }
    
    val name by viewModel.name.collectAsState()
    val alias by viewModel.alias.collectAsState()
    val roleType by viewModel.roleType.collectAsState()
    val description by viewModel.description.collectAsState()
    
    val roleTypes = listOf("主角", "配角", "反派", "其他")
    var expanded by remember { mutableStateOf(false) }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("角色管理 - ${if (characterId == null) "新建" else name}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveCharacter(onBackClick) },
                        enabled = name.isNotBlank()
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
                value = name,
                onValueChange = viewModel::setName,
                label = { Text("角色名 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = alias,
                onValueChange = viewModel::setAlias,
                label = { Text("别名/称号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = roleType,
                    onValueChange = viewModel::setRoleType,
                    label = { Text("角色类型") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = false,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    roleTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = { viewModel.setRoleType(type); expanded = false }
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = description,
                onValueChange = viewModel::setDescription,
                label = { Text("角色描述") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                maxLines = 8
            )
        }
    }
}
