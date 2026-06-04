package com.bicy.novel.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bicy.novel.data.preferences.AppSettings
import com.bicy.novel.data.preferences.ThemeType
import com.bicy.novel.ui.components.SecurityLockInputDialog
import com.bicy.novel.ui.components.SecurityQuestionDialog
import com.bicy.novel.ui.components.SecurityLockUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToLogViewer: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val dbDiagnostic by viewModel.dbDiagnostic.collectAsState()
    val logFileSizeMb by viewModel.logFileSizeMb.collectAsState()
    var showApiKey by remember { mutableStateOf(false) }
    var showDbDiagnostic by remember { mutableStateOf(false) }
    var aiExpanded by remember { mutableStateOf(false) }
    var editorExpanded by remember { mutableStateOf(false) }
    var logExpanded by remember { mutableStateOf(false) }
    var aboutExpanded by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(false) }
    var securityExpanded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    // 安全锁相关状态
    var showLockSetupDialog by remember { mutableStateOf(false) }
    var showLockVerifyDialog by remember { mutableStateOf(false) }
    var showQuestionSetupDialog by remember { mutableStateOf(false) }
    var showQuestionVerifyDialog by remember { mutableStateOf(false) }
    var showQuestionModifyDialog by remember { mutableStateOf(false) } // 修改安全问题
    var showQuestionModifyVerifyDialog by remember { mutableStateOf(false) } // 修改安全问题前的密码验证
    var lockSetupStep by remember { mutableStateOf(0) } // 0: 密码, 1: 安全问题
    var tempPassword by remember { mutableStateOf("") }
    
    // API Key 安全相关状态
    var apiKeyUnlocked by remember { mutableStateOf(false) } // API Key 是否已解锁显示
    var apiKeyEditUnlocked by remember { mutableStateOf(false) } // API Key 是否已解锁编辑
    var apiKeyVerifyFailedCount by remember { mutableStateOf(0) } // 验证失败次数
    var apiKeyLocked by remember { mutableStateOf(false) } // API Key 是否被锁定（验证失败超过5次）
    var showApiKeyVerifyDialog by remember { mutableStateOf(false) } // API Key 验证对话框
    var apiKeyVerifyPurpose by remember { mutableStateOf("") } // 验证目的：show/edit
    var copyConfirmPending by remember { mutableStateOf(false) } // 复制确认等待中
    var showCopyConfirmToast by remember { mutableStateOf(false) } // 显示复制确认提示
    var tempApiKeyForEdit by remember { mutableStateOf("") } // 编辑中的临时 API Key
    var apiKeyEditTimeout by remember { mutableStateOf(false) } // 编辑超时
    
    // API Key 验证失败锁定后的安全问题对话框
    var showApiKeyLockQuestionDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { viewModel.loadLogFileSize() }
    
    // 离开设置页面时重新上锁 API Key
    DisposableEffect(Unit) {
        onDispose {
            apiKeyUnlocked = false
            apiKeyEditUnlocked = false
            copyConfirmPending = false
        }
    }
    
    // 安全锁设置对话框
    if (showLockSetupDialog) {
        SecurityLockInputDialog(
            title = "设置安全锁",
            isRegisterMode = true,
            onConfirm = { password ->
                tempPassword = password
                showLockSetupDialog = false
                showQuestionSetupDialog = true
            },
            onDismiss = { showLockSetupDialog = false }
        )
    }
    
    // 安全问题设置对话框
    if (showQuestionSetupDialog) {
        SecurityQuestionDialog(
            isSetupMode = true,
            onConfirm = { question, answer ->
                viewModel.setSecurityLock(tempPassword, question, answer)
                showQuestionSetupDialog = false
                tempPassword = ""
            },
            onDismiss = { 
                showQuestionSetupDialog = false
                tempPassword = ""
            }
        )
    }
    
    // 安全锁验证对话框（修改密码时）
    if (showLockVerifyDialog) {
        SecurityLockInputDialog(
            title = "验证安全锁",
            subtitle = "请输入当前密码",
            isRegisterMode = false,
            onConfirm = { password ->
                if (viewModel.verifySecurityLockPassword(password)) {
                    showLockVerifyDialog = false
                    showLockSetupDialog = true
                }
            },
            onDismiss = { showLockVerifyDialog = false },
            onForgotPassword = {
                showLockVerifyDialog = false
                showQuestionVerifyDialog = true
            }
        )
    }
    
    // 修改安全问题前的密码验证对话框
    if (showQuestionModifyVerifyDialog) {
        SecurityLockInputDialog(
            title = "验证安全锁",
            subtitle = "请输入当前密码",
            isRegisterMode = false,
            onConfirm = { password ->
                if (viewModel.verifySecurityLockPassword(password)) {
                    showQuestionModifyVerifyDialog = false
                    showQuestionModifyDialog = true
                }
            },
            onDismiss = { showQuestionModifyVerifyDialog = false },
            onForgotPassword = {
                showQuestionModifyVerifyDialog = false
                showQuestionVerifyDialog = true
            }
        )
    }
    
    // 修改安全问题对话框
    if (showQuestionModifyDialog) {
        SecurityQuestionDialog(
            isSetupMode = true,
            onConfirm = { question, answer ->
                viewModel.updateSecurityQuestion(question, answer)
                showQuestionModifyDialog = false
            },
            onDismiss = { showQuestionModifyDialog = false }
        )
    }
    
    // 安全问题验证对话框（忘记密码时）
    if (showQuestionVerifyDialog) {
        SecurityQuestionDialog(
            isSetupMode = false,
            existingQuestion = settings.securityQuestion,
            onConfirm = { _, _ ->
                // 验证通过，允许设置新密码
                showQuestionVerifyDialog = false
                showLockSetupDialog = true
            },
            onDismiss = { showQuestionVerifyDialog = false },
            onVerifyAnswer = { answer -> viewModel.verifySecurityAnswer(answer) }
        )
    }
    
    // API Key 验证对话框
    if (showApiKeyVerifyDialog && settings.isSecurityLockSet) {
        SecurityLockInputDialog(
            title = "验证安全锁",
            subtitle = "请输入密码以${if (apiKeyVerifyPurpose == "show") "显示" else "编辑"} API Key",
            isRegisterMode = false,
            onConfirm = { password ->
                if (viewModel.verifySecurityLockPassword(password)) {
                    showApiKeyVerifyDialog = false
                    apiKeyVerifyFailedCount = 0
                    when (apiKeyVerifyPurpose) {
                        "show" -> {
                            apiKeyUnlocked = true
                            showApiKey = true
                        }
                        "edit" -> {
                            apiKeyEditUnlocked = true
                            tempApiKeyForEdit = settings.aiApiKey
                            apiKeyEditTimeout = false
                            // 30秒后自动锁定
                            scope.launch {
                                delay(30000)
                                if (apiKeyEditUnlocked) {
                                    apiKeyEditUnlocked = false
                                    apiKeyEditTimeout = true
                                }
                            }
                        }
                    }
                } else {
                    apiKeyVerifyFailedCount++
                    if (apiKeyVerifyFailedCount >= 5) {
                        apiKeyLocked = true
                        showApiKeyVerifyDialog = false
                        showApiKeyLockQuestionDialog = true
                    }
                }
            },
            onDismiss = { showApiKeyVerifyDialog = false },
            onForgotPassword = {
                showApiKeyVerifyDialog = false
                showApiKeyLockQuestionDialog = true
            }
        )
    }
    
    // API Key 锁定后的安全问题验证对话框
    if (showApiKeyLockQuestionDialog) {
        SecurityQuestionDialog(
            isSetupMode = false,
            existingQuestion = settings.securityQuestion,
            onConfirm = { _, _ ->
                showApiKeyLockQuestionDialog = false
                apiKeyLocked = false
                apiKeyVerifyFailedCount = 0
            },
            onDismiss = { showApiKeyLockQuestionDialog = false },
            onVerifyAnswer = { answer -> viewModel.verifySecurityAnswer(answer) }
        )
    }
    
    // 复制确认提示
    if (showCopyConfirmToast) {
        LaunchedEffect(showCopyConfirmToast) {
            delay(3000)
            showCopyConfirmToast = false
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        
        // 安全锁设置（放在最前面）
        SettingsExpandableSection(
            title = "安全锁",
            subtitle = if (settings.isSecurityLockSet) "已设置" else "未设置",
            icon = Icons.Default.Lock,
            expanded = securityExpanded,
            onExpandChange = { securityExpanded = it }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (settings.isSecurityLockSet) {
                    // 安全锁已设置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启动时需要安全验证", style = MaterialTheme.typography.bodyMedium)
                            Text("应用启动时需要输入密码", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.requireSecurityOnStartup,
                            onCheckedChange = { viewModel.updateRequireSecurityOnStartup(it) }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Button(
                        onClick = { showLockVerifyDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("修改密码")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showQuestionModifyVerifyDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("修改安全问题")
                    }
                    
                    if (settings.securityQuestion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.securityQuestion,
                            onValueChange = { },
                            label = { Text("当前安全问题") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false
                        )
                    }
                } else {
                    // 安全锁未设置
                    Text("安全锁用于保护敏感信息（如API Key），设置后可保护您的数据安全。", style = MaterialTheme.typography.bodyMedium)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showLockSetupDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("设置安全锁")
                    }
                }
            }
        }
        
        SettingsExpandableSection(
            title = "AI 配置",
            subtitle = if (settings.aiEnabled) "已启用 - ${settings.aiModel}" else "未启用",
            icon = Icons.Default.Psychology,
            expanded = aiExpanded,
            onExpandChange = { aiExpanded = it }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("启用 AI 功能", style = MaterialTheme.typography.bodyMedium)
                        Text("开启后可使用AI辅助写作", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.aiEnabled,
                        onCheckedChange = { viewModel.updateAIEnabled(it) }
                    )
                }
                
                if (settings.aiEnabled) {
                    HorizontalDivider()
                    
                    OutlinedTextField(
                        value = settings.aiApiEndpoint,
                        onValueChange = { viewModel.updateAIApiEndpoint(it) },
                        label = { Text("API 端点") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // API Key 输入框
                    if (settings.isSecurityLockSet) {
                        // 已设置安全锁，需要验证才能显示/编辑
                        Column {
                            OutlinedTextField(
                                value = if (apiKeyEditUnlocked) tempApiKeyForEdit else settings.aiApiKey,
                                onValueChange = { newValue ->
                                    if (apiKeyEditUnlocked && !apiKeyLocked) {
                                        tempApiKeyForEdit = newValue
                                    }
                                },
                                label = { 
                                    Text(if (apiKeyLocked) "API Key (已锁定)" else "API Key") 
                                },
                                placeholder = { Text("sk-...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                readOnly = !apiKeyEditUnlocked || apiKeyLocked,
                                visualTransformation = if (showApiKey && apiKeyUnlocked) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row {
                                        // 显示/隐藏按钮
                                        IconButton(onClick = {
                                            if (apiKeyLocked) {
                                                showApiKeyLockQuestionDialog = true
                                            } else if (!apiKeyUnlocked) {
                                                apiKeyVerifyPurpose = "show"
                                                showApiKeyVerifyDialog = true
                                            } else {
                                                showApiKey = !showApiKey
                                                if (!showApiKey) {
                                                    // 隐藏后重新上锁
                                                    apiKeyUnlocked = false
                                                }
                                            }
                                        }) {
                                            Icon(
                                                if (showApiKey && apiKeyUnlocked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (showApiKey && apiKeyUnlocked) "隐藏" else "显示"
                                            )
                                        }
                                        // 复制按钮
                                        IconButton(onClick = {
                                            if (apiKeyLocked) {
                                                showApiKeyLockQuestionDialog = true
                                            } else if (showApiKey && apiKeyUnlocked) {
                                                // 已显示状态，处理复制
                                                if (copyConfirmPending) {
                                                    // 第二次复制，真正复制
                                                    clipboardManager.setText(AnnotatedString(settings.aiApiKey))
                                                    copyConfirmPending = false
                                                } else {
                                                    // 第一次复制，提示确认
                                                    copyConfirmPending = true
                                                    showCopyConfirmToast = true
                                                }
                                            } else {
                                                // 隐藏状态，复制"·"
                                                clipboardManager.setText(AnnotatedString("•".repeat(settings.aiApiKey.length)))
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                                        }
                                        // 编辑按钮
                                        IconButton(onClick = {
                                            if (apiKeyLocked) {
                                                showApiKeyLockQuestionDialog = true
                                            } else if (!apiKeyEditUnlocked) {
                                                apiKeyVerifyPurpose = "edit"
                                                showApiKeyVerifyDialog = true
                                            }
                                        }) {
                                            Icon(
                                                if (apiKeyEditUnlocked) Icons.Default.LockOpen else Icons.Default.Edit,
                                                contentDescription = if (apiKeyEditUnlocked) "编辑中" else "编辑"
                                            )
                                        }
                                    }
                                }
                            )
                            
                            // 编辑中的保存按钮
                            if (apiKeyEditUnlocked) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateAIApiKey(tempApiKeyForEdit)
                                            apiKeyEditUnlocked = false
                                            tempApiKeyForEdit = ""
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("保存")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            apiKeyEditUnlocked = false
                                            tempApiKeyForEdit = ""
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("取消")
                                    }
                                }
                            }
                            
                            // 复制确认提示
                            if (showCopyConfirmToast && copyConfirmPending) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "请再次复制表示您同意本次复制，造成的 API Key 泄露请自己负责",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            
                            // 编辑超时提示
                            if (apiKeyEditTimeout) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "编辑超时，请重新验证",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                apiKeyEditTimeout = false
                            }
                        }
                    } else {
                        // 未设置安全锁，直接显示和编辑
                        OutlinedTextField(
                            value = settings.aiApiKey,
                            onValueChange = { viewModel.updateAIApiKey(it) },
                            label = { Text("API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showApiKey) "隐藏" else "显示"
                                        )
                                    }
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(settings.aiApiKey))
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                                    }
                                }
                            }
                        )
                    }
                    
                    OutlinedTextField(
                        value = settings.aiModel,
                        onValueChange = { viewModel.updateAIModel(it) },
                        label = { Text("模型") },
                        placeholder = { Text("gpt-4, claude-3-opus, 等") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Temperature: ${String.format("%.1f", settings.aiTemperature)}", style = MaterialTheme.typography.bodyMedium)
                            Text("控制输出随机性", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Slider(
                        value = settings.aiTemperature,
                        onValueChange = { viewModel.updateAITemperature(it) },
                        valueRange = 0f..2f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = settings.aiMaxTokens.toString(),
                        onValueChange = { 
                            val value = it.toIntOrNull()
                            if (value != null) {
                                viewModel.updateAIMaxTokens(value)
                            }
                        },
                        label = { Text("最大Token") },
                        placeholder = { Text("1024 - 32768") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("限制AI响应长度 (范围: 1024-32768)") }
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("最大工具调用次数: ${settings.aiMaxToolIterations}", style = MaterialTheme.typography.bodyMedium)
                            Text("Agent模式下的工具调用限制", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Slider(
                        value = settings.aiMaxToolIterations.toFloat(),
                        onValueChange = { viewModel.updateAIMaxToolIterations(it.toInt()) },
                        valueRange = 1f..100f,
                        steps = 99,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启用思考过程", style = MaterialTheme.typography.bodyMedium)
                            Text("显示AI的推理过程（需模型支持）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.aiEnableThinking,
                            onCheckedChange = { viewModel.updateAIEnableThinking(it) }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("普通模式提示词", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { viewModel.resetSystemPrompt() }) {
                                Text("恢复默认", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        OutlinedTextField(
                            value = settings.aiSystemPrompt,
                            onValueChange = { viewModel.updateAISystemPrompt(it) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text(AppSettings.DEFAULT_SYSTEM_PROMPT) },
                            supportingText = { Text("普通对话模式的系统提示词") }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Agent模式提示词", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { viewModel.resetAgentSystemPrompt() }) {
                                Text("恢复默认", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        OutlinedTextField(
                            value = settings.aiAgentSystemPrompt,
                            onValueChange = { viewModel.updateAIAgentSystemPrompt(it) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text(AppSettings.DEFAULT_AGENT_SYSTEM_PROMPT) },
                            supportingText = { Text("Agent模式的系统提示词（工具调用）") }
                        )
                    }
                }
            }
        }
        
        SettingsExpandableSection(
            title = "外观设置",
            subtitle = "主题: ${settings.getTheme().displayName}",
            icon = Icons.Default.Palette,
            expanded = appearanceExpanded,
            onExpandChange = { appearanceExpanded = it }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("护眼主题", style = MaterialTheme.typography.bodyMedium)
                Text("选择适合长时间阅读的护眼配色方案", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ThemeType.entries.forEach { theme ->
                    val isSelected = settings.themeType == theme.name
                    val backgroundColor = try { Color(android.graphics.Color.parseColor(theme.backgroundColor)) } catch (e: Exception) { Color.White }
                    val textColor = try { Color(android.graphics.Color.parseColor(theme.textColor)) } catch (e: Exception) { Color.Black }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateThemeType(theme.name) },
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(theme.displayName, style = MaterialTheme.typography.bodyMedium, color = textColor)
                                Text(
                                    when (theme) {
                                        ThemeType.CLASSIC -> "经典米白色背景，适合长时间阅读"
                                        ThemeType.EYE_GREEN -> "淡绿色背景，缓解眼睛疲劳"
                                        ThemeType.EYE_BLUE -> "淡蓝色背景，冷静专注"
                                        ThemeType.EYE_GRAY -> "中性灰色背景，简洁护眼"
                                        ThemeType.DARK -> "深色背景，夜间阅读推荐"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "已选择", tint = textColor)
                            }
                        }
                    }
                }
            }
        }
        
        SettingsExpandableSection(
            title = "编辑器设置",
            subtitle = "字号: ${settings.editorFontSize}sp",
            icon = Icons.Default.Edit,
            expanded = editorExpanded,
            onExpandChange = { editorExpanded = it }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("字体大小: ${settings.editorFontSize}sp", style = MaterialTheme.typography.bodyMedium)
                        Text("调整编辑器文字大小", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Slider(
                    value = settings.editorFontSize.toFloat(),
                    onValueChange = { viewModel.updateEditorFontSize(it.toInt()) },
                    valueRange = 12f..24f,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                
                HorizontalDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("显示行号", style = MaterialTheme.typography.bodyMedium)
                        Text("在编辑器左侧显示行号", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.editorShowLineNumbers,
                        onCheckedChange = { viewModel.updateEditorShowLineNumbers(it) }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("自动换行", style = MaterialTheme.typography.bodyMedium)
                        Text("长文本自动换行显示", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.editorAutoWrap,
                        onCheckedChange = { viewModel.updateEditorAutoWrap(it) }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("自动保存", style = MaterialTheme.typography.bodyMedium)
                        Text("编辑时自动保存内容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = settings.editorAutoSave,
                        onCheckedChange = { viewModel.updateEditorAutoSave(it) }
                    )
                }
            }
        }
        
        SettingsExpandableSection(
            title = "日志与数据",
            subtitle = if (settings.logEnabled) "日志记录已开启" else "日志记录已关闭",
            icon = Icons.Default.Storage,
            expanded = logExpanded,
            onExpandChange = { logExpanded = it }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("日志记录", style = MaterialTheme.typography.bodyMedium)
                        Text("实时记录应用到外部存储", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("路径: Android/data/包名/files/log/", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = settings.logEnabled,
                        onCheckedChange = { viewModel.updateLogEnabled(it) }
                    )
                }
                
                HorizontalDivider()
                
                Text("关闭日志记录不影响崩溃日志记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Button(
                    onClick = onNavigateToLogViewer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("查看日志")
                }
                
                OutlinedButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        viewModel.loadLogFileSize()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清理日志 (${String.format("%.2f", logFileSizeMb)} MB)")
                }
                
                Button(
                    onClick = {
                        viewModel.runDbDiagnose()
                        showDbDiagnostic = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("诊断数据库")
                }
                
                Button(
                    onClick = { viewModel.runVacuum() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VACUUM 压缩数据库")
                }
            }
        }
        
        SettingsExpandableSection(
            title = "关于",
            subtitle = "NovelEditor v1.0.0",
            icon = Icons.Default.Info,
            expanded = aboutExpanded,
            onExpandChange = { aboutExpanded = it }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "NovelEditor 是一款专为小说创作者设计的编辑器应用。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "功能特点：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text("• 纯文本存储，轻量化设计", style = MaterialTheme.typography.bodySmall)
                Text("• 支持多卷多章节管理", style = MaterialTheme.typography.bodySmall)
                Text("• 角色、世界观、笔记、时间线管理", style = MaterialTheme.typography.bodySmall)
                Text("• AI辅助写作（需配置API）", style = MaterialTheme.typography.bodySmall)
                Text("• 搜索替换、撤销重做", style = MaterialTheme.typography.bodySmall)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "版本: 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
    }
    
    // 诊断对话框
    if (showDbDiagnostic) {
        AlertDialog(
            onDismissRequest = { showDbDiagnostic = false },
            title = { Text("数据库诊断") },
            text = {
                Text(
                    text = dbDiagnostic ?: "加载中...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            },
            confirmButton = {
                TextButton(onClick = { showDbDiagnostic = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
fun SettingsExpandableSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开"
                )
            }
            
            if (expanded) {
                HorizontalDivider()
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}
