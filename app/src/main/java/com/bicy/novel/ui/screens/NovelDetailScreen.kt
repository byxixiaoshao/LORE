package com.bicy.novel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bicy.novel.ui.components.LoadingState
import com.bicy.novel.ui.components.SecurityLockInputDialog
import com.bicy.novel.ui.components.SecurityQuestionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelId: Long,
    onBackClick: () -> Unit,
    onEditNovelClick: () -> Unit,
    onEditChapterClick: (Long?) -> Unit,
    onEditCharacterClick: (Long?) -> Unit,
    onEditWorldviewClick: (Long?) -> Unit,
    onEditNoteClick: (Long?) -> Unit,
    onEditTimelineClick: (Long?) -> Unit,
    onExportClick: () -> Unit,
    viewModel: NovelDetailViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.loadNovel(novelId)
    }
    
    val novel by viewModel.novel.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // 安全锁验证状态
    var showLockVerifyDialog by remember { mutableStateOf(false) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var verifyFailedCount by remember { mutableStateOf(0) }
    var isVerified by remember { mutableStateOf(false) }
    
    // 检查项目是否被锁定，如果是则弹出验证
    LaunchedEffect(novel, settings.isSecurityLockSet) {
        if (novel != null && novel!!.isLocked && settings.isSecurityLockSet && !isVerified) {
            showLockVerifyDialog = true
        }
    }
    
    // 密码验证对话框
    if (showLockVerifyDialog && settings.isSecurityLockSet) {
        SecurityLockInputDialog(
            title = "验证安全锁",
            subtitle = "项目已被锁定，请输入密码",
            isRegisterMode = false,
            onConfirm = { password ->
                if (settingsViewModel.verifySecurityLockPassword(password)) {
                    showLockVerifyDialog = false
                    verifyFailedCount = 0
                    isVerified = true
                } else {
                    verifyFailedCount++
                    if (verifyFailedCount >= 5) {
                        showLockVerifyDialog = false
                        showQuestionDialog = true
                    }
                }
            },
            onDismiss = { 
                showLockVerifyDialog = false
                onBackClick()
            },
            onForgotPassword = {
                showLockVerifyDialog = false
                showQuestionDialog = true
            }
        )
    }
    
    // 安全问题验证对话框
    if (showQuestionDialog) {
        SecurityQuestionDialog(
            isSetupMode = false,
            existingQuestion = settings.securityQuestion,
            onConfirm = { _, _ ->
                showQuestionDialog = false
                verifyFailedCount = 0
                isVerified = true
            },
            onDismiss = { 
                showQuestionDialog = false
                onBackClick()
            },
            onVerifyAnswer = { answer -> settingsViewModel.verifySecurityAnswer(answer) }
        )
    }
    
    var selectedTab by remember { mutableStateOf(0) }
    var showNewChapterDialog by remember { mutableStateOf(false) }
    val tabs = listOf("概览", "内容", "笔记", "百科", "时间线")
    val tabIcons = listOf(
        Icons.Default.Info,
        Icons.AutoMirrored.Filled.MenuBook,
        Icons.Default.Lightbulb,
        Icons.Default.Book,
        Icons.Default.Timeline
    )
    
    if (isLoading) {
        LoadingState(modifier = Modifier.fillMaxSize())
        return
    }
    
    // 如果项目被锁定且未验证，不显示内容
    if (novel != null && novel!!.isLocked && settings.isSecurityLockSet && !isVerified) {
        return
    }
    
    if (showNewChapterDialog) {
        AlertDialog(
            onDismissRequest = { showNewChapterDialog = false },
            title = { Text("新建章节") },
            text = { Text("当前小说还没有章节，是否创建新章节？") },
            confirmButton = {
                TextButton(onClick = {
                    showNewChapterDialog = false
                    onEditChapterClick(null)
                }) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChapterDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text(tabs[selectedTab]) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tabIcons[index], contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> OverviewScreen(
                novelId = novelId,
                onStartEditClick = { id, type ->
                    when (type) {
                        "CHAPTER" -> onEditChapterClick(id)
                        "CHARACTER" -> onEditCharacterClick(id)
                        "WORLDVIEW" -> onEditWorldviewClick(id)
                        "NOTE" -> onEditNoteClick(id)
                        "TIMELINE" -> onEditTimelineClick(id)
                        else -> onEditChapterClick(id)
                    }
                },
                onEditNovelClick = onEditNovelClick,
                onShowNewChapterDialog = { showNewChapterDialog = true },
                onExportClick = onExportClick,
                modifier = Modifier.padding(padding)
            )
            1 -> ContentScreen(
                novelId = novelId,
                onChapterClick = { chapterId -> onEditChapterClick(chapterId) },
                modifier = Modifier.padding(padding)
            )
            2 -> NotesScreen(
                novelId = novelId,
                onNoteClick = { noteId -> onEditNoteClick(noteId) },
                modifier = Modifier.padding(padding)
            )
            3 -> WikiScreen(
                novelId = novelId,
                onCharacterClick = { characterId -> onEditCharacterClick(characterId) },
                onWorldviewClick = { worldviewId -> onEditWorldviewClick(worldviewId) },
                modifier = Modifier.padding(padding)
            )
            4 -> TimelineScreen(
                novelId = novelId,
                onEventClick = { eventId -> onEditTimelineClick(eventId) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}
