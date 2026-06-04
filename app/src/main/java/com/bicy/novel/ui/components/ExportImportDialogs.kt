package com.bicy.novel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

data class ExportOptions(
    val password: String = "",
    val includeChapters: Boolean = true,
    val includeCharacters: Boolean = true,
    val includeWorldviews: Boolean = true,
    val includeNotes: Boolean = true,
    val includeTimeline: Boolean = true,
    val isBackup: Boolean = false  // true=JSON备份, false=TXT导出
)

@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExportOptions) -> Unit,
    isExporting: Boolean = false
) {
    var exportType by remember { mutableStateOf(0) } // 0=正式导出(TXT), 1=数据备份(JSON)
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var usePassword by remember { mutableStateOf(false) }
    
    var includeChapters by remember { mutableStateOf(true) }
    var includeCharacters by remember { mutableStateOf(true) }
    var includeWorldviews by remember { mutableStateOf(true) }
    var includeNotes by remember { mutableStateOf(true) }
    var includeTimeline by remember { mutableStateOf(true) }
    
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出项目") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 导出类型选择
                Text(
                    text = "导出类型",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportType == 0,
                            onClick = { exportType = 0 }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("正式导出 (TXT)")
                            Text(
                                text = "人类可读格式，适合阅读和分享",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = exportType == 1,
                            onClick = { exportType = 1 }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("数据备份 (JSON)")
                            Text(
                                text = "完整数据格式，支持导入恢复",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                
                Text(
                    text = "导出内容",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeChapters,
                            onCheckedChange = { includeChapters = it }
                        )
                        Text("章节内容")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeCharacters,
                            onCheckedChange = { includeCharacters = it }
                        )
                        Text("角色设定")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeWorldviews,
                            onCheckedChange = { includeWorldviews = it }
                        )
                        Text("世界观设定")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeNotes,
                            onCheckedChange = { includeNotes = it }
                        )
                        Text("笔记")
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeTimeline,
                            onCheckedChange = { includeTimeline = it }
                        )
                        Text("时间线")
                    }
                }
                
                HorizontalDivider()
                
                // 密码保护仅对JSON备份有效
                if (exportType == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = usePassword,
                            onCheckedChange = { 
                                usePassword = it
                                if (!it) {
                                    password = ""
                                    confirmPassword = ""
                                    passwordError = null
                                }
                            }
                        )
                        Text("设置密码保护")
                    }
                    
                    if (usePassword) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { 
                                    password = it
                                    passwordError = null
                                },
                                label = { Text("密码") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) 
                                    VisualTransformation.None 
                                else 
                                    PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff 
                                            else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                isError = passwordError != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { 
                                    confirmPassword = it
                                    passwordError = null
                                },
                                label = { Text("确认密码") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) 
                                    VisualTransformation.None 
                                else 
                                    PasswordVisualTransformation(),
                                isError = passwordError != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            if (passwordError != null) {
                                Text(
                                    text = passwordError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    // TXT导出提示
                    Text(
                        text = "提示：TXT格式导出仅供阅读和分享，不支持导入恢复",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (usePassword && exportType == 1) {
                        if (password.isBlank()) {
                            passwordError = "密码不能为空"
                            return@Button
                        }
                        if (password != confirmPassword) {
                            passwordError = "两次输入的密码不一致"
                            return@Button
                        }
                    }
                    
                    onConfirm(ExportOptions(
                        password = if (usePassword && exportType == 1) password else "",
                        includeChapters = includeChapters,
                        includeCharacters = includeCharacters,
                        includeWorldviews = includeWorldviews,
                        includeNotes = includeNotes,
                        includeTimeline = includeTimeline,
                        isBackup = exportType == 1
                    ))
                },
                enabled = !isExporting
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ImportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isImporting: Boolean = false
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("输入密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("此项目已加密，请输入密码以解密导入")
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff 
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank() && !isImporting
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text("取消")
            }
        }
    )
}
