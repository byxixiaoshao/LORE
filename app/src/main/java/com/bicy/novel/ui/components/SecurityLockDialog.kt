package com.bicy.novel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bicy.novel.util.CryptoUtils

/**
 * 密码加密工具
 */
object SecurityLockUtils {
    fun encryptPassword(password: String): String {
        if (password.isEmpty()) return ""
        return CryptoUtils.encrypt(password)
    }
    
    fun decryptPassword(encrypted: String): String {
        if (encrypted.isEmpty()) return ""
        return try {
            CryptoUtils.decrypt(encrypted)
        } catch (e: Exception) {
            ""
        }
    }
    
    fun verifyPassword(password: String, encrypted: String): Boolean {
        if (password.isEmpty() || encrypted.isEmpty()) return false
        val decrypted = decryptPassword(encrypted)
        return password == decrypted
    }
}

/**
 * 密码输入/注册窗口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityLockInputDialog(
    title: String = "设置安全锁",
    subtitle: String = "请输入密码",
    isRegisterMode: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit = {},
    onForgotPassword: (() -> Unit)? = null,
    showCancelButton: Boolean = true
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (isRegisterMode) 0 else 1) } // 0: 输入, 1: 确认
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 锁图标
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isRegisterMode) {
                        if (step == 0) "请输入密码" else "请再次输入确认"
                    } else {
                        subtitle
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 密码显示区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showPassword) {
                            if (step == 0) password else confirmPassword
                        } else {
                            "•".repeat(if (step == 0) password.length else confirmPassword.length)
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        letterSpacing = 8.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 显示/隐藏密码按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "隐藏" else "显示"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showPassword) "隐藏" else "显示")
                    }
                }
                
                // 数字键盘
                Spacer(modifier = Modifier.height(8.dp))
                
                val buttons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("删除", "0", "确认")
                )
                
                buttons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { btn ->
                            val isSpecial = btn == "删除" || btn == "确认"
                            val isDelete = btn == "删除"
                            val isConfirm = btn == "确认"
                            
                            Button(
                                onClick = {
                                    when {
                                        isDelete -> {
                                            if (step == 0) {
                                                if (password.isNotEmpty()) {
                                                    password = password.dropLast(1)
                                                }
                                            } else {
                                                if (confirmPassword.isNotEmpty()) {
                                                    confirmPassword = confirmPassword.dropLast(1)
                                                }
                                            }
                                            errorMessage = ""
                                        }
                                        isConfirm -> {
                                            val currentPwd = if (step == 0) password else confirmPassword
                                            if (currentPwd.isEmpty()) {
                                                errorMessage = "请输入密码"
                                            } else if (isRegisterMode && step == 0) {
                                                // 进入确认步骤
                                                step = 1
                                                errorMessage = ""
                                            } else if (isRegisterMode && step == 1) {
                                                // 验证两次密码是否一致
                                                if (password != confirmPassword) {
                                                    errorMessage = "两次密码不一致"
                                                    confirmPassword = ""
                                                } else {
                                                    onConfirm(password)
                                                }
                                            } else {
                                                // 非注册模式，直接确认
                                                onConfirm(currentPwd)
                                            }
                                        }
                                        else -> {
                                            if (step == 0) {
                                                password += btn
                                            } else {
                                                confirmPassword += btn
                                            }
                                            errorMessage = ""
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = if (isConfirm) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                } else if (isDelete) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                }
                            ) {
                                when {
                                    isDelete -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "删除")
                                    isConfirm -> Icon(Icons.Default.Check, contentDescription = "确认")
                                    else -> Text(btn, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // 错误信息
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // 忘记密码按钮
                if (onForgotPassword != null && !isRegisterMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onForgotPassword) {
                        Text("忘记密码？")
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            if (showCancelButton) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

/**
 * 安全问题设置/验证窗口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionDialog(
    isSetupMode: Boolean = true,
    existingQuestion: String = "",
    onConfirm: (String, String) -> Unit, // (question, answer)
    onDismiss: () -> Unit,
    onVerifyAnswer: ((String) -> Boolean)? = null
) {
    var question by remember { mutableStateOf(existingQuestion) }
    var answer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    val presetQuestions = listOf(
        "你的出生地是哪里？",
        "你母亲的姓氏是什么？",
        "你第一只宠物的名字是什么？",
        "你毕业的小学名称是什么？",
        "你最喜爱的电影是什么？"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSetupMode) "设置安全问题" else "验证安全问题") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isSetupMode) {
                    // 设置模式：选择或输入问题
                    Text("选择或输入安全问题：", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 预设问题列表
                    presetQuestions.forEach { q ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = question == q,
                                onClick = { question = q }
                            )
                            Text(q, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 自定义问题
                    OutlinedTextField(
                        value = if (question !in presetQuestions) question else "",
                        onValueChange = { question = it },
                        label = { Text("或输入自定义问题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    // 验证模式：显示问题
                    Text(
                        text = existingQuestion,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 答案输入
                OutlinedTextField(
                    value = answer,
                    onValueChange = { 
                        answer = it
                        errorMessage = ""
                    },
                    label = { Text(if (isSetupMode) "输入答案" else "输入答案验证") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty()
                )
                
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    isSetupMode -> {
                        if (question.isEmpty()) {
                            errorMessage = "请选择或输入问题"
                        } else if (answer.isEmpty()) {
                            errorMessage = "请输入答案"
                        } else {
                            onConfirm(question, answer)
                        }
                    }
                    else -> {
                        if (answer.isEmpty()) {
                            errorMessage = "请输入答案"
                        } else if (onVerifyAnswer?.invoke(answer) == true) {
                            onConfirm(question, answer)
                        } else {
                            errorMessage = "答案错误"
                        }
                    }
                }
            }) {
                Text(if (isSetupMode) "确定" else "验证")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
