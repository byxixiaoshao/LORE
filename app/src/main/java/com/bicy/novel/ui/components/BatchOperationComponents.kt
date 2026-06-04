package com.bicy.novel.ui.components

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

/**
 * 批量操作组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BatchOperationSheet(
    items: List<T>,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String = { "" },
    selectedIds: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
    title: String = "批量操作",
    confirmButtonText: String = "确定",
    itemId: (T) -> Long,
    modifier: Modifier = Modifier
) {
    var selectAll by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedIds, items.size) {
        selectAll = selectedIds.size == items.size && items.isNotEmpty()
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            },
            actions = {
                // 全选/取消全选
                TextButton(onClick = {
                    if (selectAll) {
                        onSelectionChange(emptySet())
                    } else {
                        onSelectionChange(items.map { itemId(it) }.toSet())
                    }
                }) {
                    Text(if (selectAll) "取消全选" else "全选")
                }
            }
        )
        
        // 已选数量提示
        if (selectedIds.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "已选择 ${selectedIds.size} 项",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // 列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(items) { item ->
                val id = itemId(item)
                val isSelected = selectedIds.contains(id)
                
                ListItem(
                    headlineContent = { Text(itemTitle(item)) },
                    supportingContent = { 
                        val subtitle = itemSubtitle(item)
                        if (subtitle.isNotEmpty()) {
                            Text(subtitle) 
                        }
                    },
                    leadingContent = {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                val newSelection = if (checked) {
                                    selectedIds + id
                                } else {
                                    selectedIds - id
                                }
                                onSelectionChange(newSelection)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = if (isSelected) 2.dp else 0.dp
                )
            }
        }
        
        // 底部操作栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                
                Button(
                    onClick = { onConfirm(selectedIds.toList()) },
                    modifier = Modifier.weight(1f),
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Text(confirmButtonText)
                }
            }
        }
    }
}

/**
 * 批量查找替换对话框
 */
@Composable
fun BatchFindReplaceDialog(
    onDismiss: () -> Unit,
    onConfirm: (searchText: String, replaceText: String, scope: ReplaceScope) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf(ReplaceScope.CURRENT_CHAPTER) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量查找替换") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("查找内容") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = { replaceText = it },
                    label = { Text("替换为") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text("替换范围", style = MaterialTheme.typography.labelLarge)
                
                Column {
                    ReplaceScope.values().forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = scope == item,
                                onClick = { scope = item }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(searchText, replaceText, scope) },
                enabled = searchText.isNotBlank()
            ) {
                Text("替换")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

enum class ReplaceScope(val label: String) {
    CURRENT_CHAPTER("当前章节"),
    ALL_CHAPTERS("所有章节"),
    ALL_CONTENT("所有内容（章节、角色、世界观等）")
}
