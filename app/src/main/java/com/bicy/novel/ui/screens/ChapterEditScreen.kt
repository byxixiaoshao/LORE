package com.bicy.novel.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bicy.novel.ui.components.*
import com.bicy.novel.util.WordCounter

enum class QuickNavSection(val label: String, val icon: ImageVector) {
    WORLDVIEW("世界观", Icons.Default.Public),
    CHARACTER("角色", Icons.Default.Person),
    CONTENT("正文", Icons.AutoMirrored.Filled.Article),
    NOTE("笔记", Icons.Default.Lightbulb),
    TIMELINE("时间线", Icons.Default.Timeline),
    AI("AI", Icons.Default.Psychology)
}

enum class AIMode {
    CHAT,
    AGENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterEditScreen(
    novelId: Long,
    chapterId: Long?,
    onBackClick: () -> Unit,
    onNavigateToChapter: (Long) -> Unit = {},
    onNavigateToCharacter: (Long) -> Unit = {},
    onNavigateToWorldview: (Long) -> Unit = {},
    onNavigateToNote: (Long) -> Unit = {},
    onNavigateToTimeline: (Long) -> Unit = {},
    viewModel: ChapterEditViewModel = hiltViewModel(),
    quickAccessViewModel: QuickAccessViewModel = hiltViewModel(),
    aiChatViewModel: AIChatViewModel = hiltViewModel()
) {
    var initialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(chapterId, novelId) {
        if (!initialized) {
            viewModel.init(novelId, chapterId)
            quickAccessViewModel.loadNovelId(novelId)
            aiChatViewModel.setNovelId(novelId)
            initialized = true
        } else if ((chapterId ?: 0L) != viewModel.currentId) {
            // 导航参数变化时重新加载
            viewModel.init(novelId, chapterId)
        }
    }
    
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val wordCount by viewModel.wordCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val hasDraft by viewModel.hasDraft.collectAsState()
    val editType by viewModel.editType.collectAsState()
    val unsavedDraftCount by viewModel.unsavedDraftCount.collectAsState()
    val aiChatState by aiChatViewModel.uiState.collectAsState()
    val streamingMessage by aiChatViewModel.streamingMessage.collectAsState()
    val showLineNumbers by viewModel.showLineNumbers.collectAsState()
    val editorFontSize by viewModel.editorFontSize.collectAsState()
    val editorAutoWrap by viewModel.editorAutoWrap.collectAsState()
    
    LaunchedEffect(aiChatState.refreshTrigger) {
        if (aiChatState.refreshTrigger > 0 && chapterId != null && chapterId > 0) {
            viewModel.reloadFromDatabase(chapterId)
        }
    }
    
    var showExitDialog by remember { mutableStateOf(false) }
    var showAiExitConfirmDialog by remember { mutableStateOf(false) }
    var unsavedDrafts by remember { mutableStateOf<List<com.bicy.novel.domain.model.Draft>>(emptyList()) }
    var showShortcutPanel by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(editorFontSize.toFloat()) }
    var autoWrap by remember { mutableStateOf(editorAutoWrap) }
    var currentNavSection by remember { mutableStateOf(QuickNavSection.CONTENT) }
    var scale by remember { mutableStateOf(1f) }
    
    var showSearchReplace by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var currentSearchIndex by remember { mutableStateOf(0) }
    
    val coroutineScope = rememberCoroutineScope()
    
    var aiMode by remember { mutableStateOf(AIMode.CHAT) }
    var aiInput by remember { mutableStateOf("") }
    var aiEditPreview by remember { mutableStateOf(AIEditPreview()) }
    var showAIPreview by remember { mutableStateOf(false) }
    var selectedTextForAI by remember { mutableStateOf<String?>(null) }
    
    var globalSearchQuery by remember { mutableStateOf("") }
    
    val searchResults = remember(content, searchQuery) {
        if (searchQuery.isEmpty()) emptyList()
        else {
            val results = mutableListOf<Int>()
            var index = content.indexOf(searchQuery)
            while (index >= 0) {
                results.add(index)
                index = content.indexOf(searchQuery, index + 1)
            }
            results
        }
    }
    
    BackHandler(enabled = true) {
        when {
            showShortcutPanel -> {
                showShortcutPanel = false
            }
            aiChatState.isLoading -> {
                showAiExitConfirmDialog = true
            }
            hasUnsavedChanges || hasDraft || unsavedDraftCount > 0 -> {
                coroutineScope.launch {
                    unsavedDrafts = viewModel.getUnsavedDrafts()
                    showExitDialog = true
                }
            }
            else -> {
                onBackClick()
            }
        }
    }
    
    if (showAiExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAiExitConfirmDialog = false },
            title = { Text("确认退出") },
            text = { Text("AI 正在工作中，退出编辑会中断 AI 工作。确定要退出吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAiExitConfirmDialog = false
                        onBackClick()
                    }
                ) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiExitConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                IconButton(
                    onClick = { showShortcutPanel = !showShortcutPanel },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (showShortcutPanel) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                        contentDescription = if (showShortcutPanel) "隐藏快捷栏" else "显示快捷栏"
                    )
                }
                
                HorizontalDivider()
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { viewModel.undo() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销")
                    }
                    IconButton(onClick = { viewModel.redo() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "重做")
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    // 历史按钮（仅编辑已存在的内容时显示）
                    if (viewModel.currentId > 0) {
                        IconButton(onClick = { 
                            val targetType = when (editType) {
                                EditType.CHAPTER -> "chapter"
                                EditType.CHARACTER -> "character"
                                EditType.WORLDVIEW -> "worldview"
                                EditType.NOTE -> "note"
                                EditType.TIMELINE -> "timeline"
                            }
                            showHistoryDialog = true
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.History, contentDescription = "查看历史")
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    IconButton(onClick = { showSearchReplace = !showSearchReplace }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Search, contentDescription = "搜索替换")
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    IconButton(onClick = { if (fontSize > 12f) fontSize -= 2f }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "缩小")
                    }
                    IconButton(onClick = { if (fontSize < 32f) fontSize += 2f }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "放大")
                    }
                    Text(
                        "${fontSize.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    IconButton(onClick = { autoWrap = !autoWrap }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            if (autoWrap) Icons.AutoMirrored.Filled.WrapText else Icons.Default.ViewColumn,
                            contentDescription = if (autoWrap) "自动换行" else "不换行"
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    // 保存按钮
                    IconButton(
                        onClick = { viewModel.saveChapter {} }, 
                        enabled = title.isNotBlank(),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f).statusBarsPadding()) {
            TopAppBar(
                title = { 
                    val typeLabel = when (editType) {
                        EditType.CHAPTER -> "章节"
                        EditType.CHARACTER -> "角色"
                        EditType.WORLDVIEW -> "世界观"
                        EditType.NOTE -> "笔记"
                        EditType.TIMELINE -> "时间线"
                    }
                    Text("$typeLabel - ${if (viewModel.currentId == 0L) "新建" else title}", style = MaterialTheme.typography.titleSmall) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (hasUnsavedChanges) showExitDialog = true else onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Text(
                        "${wordCount}字", 
                        style = MaterialTheme.typography.labelMedium, 
                        color = if (hasDraft || hasUnsavedChanges) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (isSaving) { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                }
            )
            
            if (showSearchReplace) {
                Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 2.dp) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it; currentSearchIndex = 0 }, placeholder = { Text("搜索") }, modifier = Modifier.weight(1f), singleLine = true)
                            Text(if (searchResults.isEmpty()) "0/0" else "${currentSearchIndex + 1}/${searchResults.size}", style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = { if (currentSearchIndex > 0) currentSearchIndex-- else if (searchResults.isNotEmpty()) currentSearchIndex = searchResults.size - 1 }) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上一个") }
                            IconButton(onClick = { if (currentSearchIndex < searchResults.size - 1) currentSearchIndex++ else if (searchResults.isNotEmpty()) currentSearchIndex = 0 }) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下一个") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = replaceQuery, onValueChange = { replaceQuery = it }, placeholder = { Text("替换") }, modifier = Modifier.weight(1f), singleLine = true)
                            Button(onClick = { if (searchResults.isNotEmpty() && searchQuery.isNotEmpty()) { val idx = searchResults[currentSearchIndex]; viewModel.setContent(content.substring(0, idx) + replaceQuery + content.substring(idx + searchQuery.length)) } }) { Text("替换") }
                            Button(onClick = { if (searchQuery.isNotEmpty()) viewModel.setContent(content.replace(searchQuery, replaceQuery)) }) { Text("全部") }
                            IconButton(onClick = { showSearchReplace = false }) { Icon(Icons.Default.Close, contentDescription = "关闭") }
                        }
                    }
                }
            }
            
            // 历史记录浮窗
            if (showHistoryDialog && viewModel.currentId > 0) {
                val targetType = when (editType) {
                    EditType.CHAPTER -> "chapter"
                    EditType.CHARACTER -> "character"
                    EditType.WORLDVIEW -> "worldview"
                    EditType.NOTE -> "note"
                    EditType.TIMELINE -> "timeline"
                }
                ContentHistoryDialog(
                    targetId = viewModel.currentId,
                    targetType = targetType,
                    onDismiss = { showHistoryDialog = false },
                    onRestored = {
                        showHistoryDialog = false
                        viewModel.reloadChapter(viewModel.currentId)
                    }
                )
            }
            
            Column(modifier = Modifier.fillMaxSize().imePadding().padding(16.dp)) {
                val titlePlaceholder = when (editType) {
                    EditType.CHAPTER -> "章节标题"
                    EditType.CHARACTER -> "角色名称"
                    EditType.WORLDVIEW -> "世界观标题"
                    EditType.NOTE -> "笔记标题"
                    EditType.TIMELINE -> "事件标题"
                }
                
                OutlinedTextField(
                    value = title,
                    onValueChange = viewModel::setTitle,
                    placeholder = { Text(titlePlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium
                )
                
                if (editType == EditType.CHAPTER) {
                    val chapters by quickAccessViewModel.chapters.collectAsState()
                    val sortedChapters = remember(chapters) { chapters.sortedBy { it.sortOrder } }
                    val currentChapterId = viewModel.currentId
                    val currentIndex = remember(sortedChapters, currentChapterId) { 
                        sortedChapters.indexOfFirst { it.id == currentChapterId } 
                    }
                    
                    if (currentIndex >= 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "第 ${currentIndex + 1} 章 / 共 ${sortedChapters.size} 章",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("搜索结果: ${currentSearchIndex + 1}/${searchResults.size}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (showAIPreview && aiEditPreview.changes.isNotEmpty()) {
                    AIEditPreviewPanel(
                        preview = aiEditPreview,
                        onApply = {
                            val lines = content.lines().toMutableList()
                            aiEditPreview.changes.sortedByDescending { it.lineNumber }.forEach { change ->
                                val idx = change.lineNumber - 1
                                when (change.type) {
                                    AILineChangeType.ADD -> {
                                        if (idx <= lines.size && change.newContent != null) {
                                            lines.add(idx, change.newContent)
                                        }
                                    }
                                    AILineChangeType.DELETE -> {
                                        if (idx in lines.indices) {
                                            lines.removeAt(idx)
                                        }
                                    }
                                    AILineChangeType.MODIFY -> {
                                        if (idx in lines.indices && change.newContent != null) {
                                            lines[idx] = change.newContent
                                        }
                                    }
                                    AILineChangeType.NONE -> {}
                                }
                            }
                            viewModel.setContent(lines.joinToString("\n"))
                            aiEditPreview = AIEditPreview()
                            showAIPreview = false
                        },
                        onReject = {
                            aiEditPreview = AIEditPreview()
                            showAIPreview = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val newFontSize = fontSize * zoom
                                fontSize = newFontSize.coerceIn(12f, 32f)
                            }
                        }
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        RichTextEditor(
                            text = content,
                            onTextChange = viewModel::setContent,
                            modifier = Modifier.fillMaxSize(),
                            fontSize = fontSize.toInt(),
                            searchQuery = searchQuery,
                            currentSearchIndex = currentSearchIndex,
                            aiEditPreview = aiEditPreview,
                            showLineNumbers = showLineNumbers,
                            showAIPreview = showAIPreview,
                            autoWrap = autoWrap,
                            onSelectionChange = { selected ->
                                selectedTextForAI = selected
                            }
                        )
                    }
                }
            }
        }
        }
        
        AnimatedVisibility(
            visible = showShortcutPanel,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .offset(x = 48.dp)
                    .width(320.dp)
                    .fillMaxHeight()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("快捷栏", style = MaterialTheme.typography.titleSmall)
                        
                        // 全局搜索栏
                        OutlinedTextField(
                            value = globalSearchQuery,
                            onValueChange = { globalSearchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            placeholder = { Text("搜索", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜索",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (globalSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { globalSearchQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "清除",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        
                        IconButton(onClick = { showShortcutPanel = false }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        QuickNavSection.entries.forEach { section ->
                            FilterChip(
                                selected = currentNavSection == section,
                                onClick = { currentNavSection = section },
                                label = { Icon(section.icon, contentDescription = section.label, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    Box(modifier = Modifier.weight(1f)) {
                        QuickAccessContent(
                            section = currentNavSection,
                            novelId = novelId,
                            novelTitle = "",
                            currentChapterId = if (editType == EditType.CHAPTER) viewModel.currentId else 0,
                            currentChapterTitle = if (editType == EditType.CHAPTER) title else "",
                            viewModel = quickAccessViewModel,
                            aiChatViewModel = aiChatViewModel,
                            searchQuery = globalSearchQuery,
                            onChapterClick = { newChapterId ->
                                onNavigateToChapter(newChapterId)
                            },
                            onCharacterClick = { characterId ->
                                onNavigateToCharacter(characterId)
                            },
                            onWorldviewClick = { worldviewId ->
                                onNavigateToWorldview(worldviewId)
                            },
                            onNoteClick = { noteId ->
                                onNavigateToNote(noteId)
                            },
                            onTimelineClick = { eventId ->
                                onNavigateToTimeline(eventId)
                            },
                            aiMode = aiMode,
                            onAiModeChange = { aiMode = it },
                            aiInput = aiInput,
                            onAiInputChange = { aiInput = it },
                            currentContent = content,
                            selectedText = selectedTextForAI,
                            onSelectedTextChange = { selectedTextForAI = it },
                            aiEditPreview = aiEditPreview,
                            onAiEditPreviewChange = { aiEditPreview = it },
                            showAIPreview = showAIPreview,
                            onShowAIPreviewChange = { showAIPreview = it }
                        )
                    }
                }
            }
        }
    }
    
    if (showExitDialog) {
        val draftList = unsavedDrafts
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("您有以下内容未保存：") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (draftList.isEmpty()) {
                        Text("当前正在编辑的内容", style = MaterialTheme.typography.bodySmall)
                    } else {
                        draftList.forEach { draft ->
                            val typeLabel = when (draft.targetType) {
                                "chapter" -> "章节"
                                "character" -> "角色"
                                "worldview" -> "世界观"
                                "note" -> "笔记"
                                "timeline" -> "时间线"
                                else -> draft.targetType
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "$typeLabel: ${draft.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${draft.wordCount}字",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "确定退出吗？",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            viewModel.saveAllDrafts()
                            showExitDialog = false
                            onBackClick()
                        }
                    }) { Text("保存并退出", color = MaterialTheme.colorScheme.primary) }
                    TextButton(onClick = {
                        showExitDialog = false
                        onBackClick()
                    }) { Text("退出", color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAccessContent(
    section: QuickNavSection,
    novelId: Long,
    novelTitle: String = "",
    currentChapterId: Long = 0,
    currentChapterTitle: String = "",
    viewModel: QuickAccessViewModel,
    aiChatViewModel: AIChatViewModel,
    searchQuery: String = "",
    onChapterClick: (Long) -> Unit,
    onCharacterClick: (Long) -> Unit,
    onWorldviewClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    onTimelineClick: (Long) -> Unit,
    aiMode: AIMode,
    onAiModeChange: (AIMode) -> Unit,
    aiInput: String,
    onAiInputChange: (String) -> Unit,
    currentContent: String = "",
    selectedText: String? = null,
    onSelectedTextChange: (String?) -> Unit = {},
    aiEditPreview: AIEditPreview = AIEditPreview(),
    onAiEditPreviewChange: (AIEditPreview) -> Unit = {},
    showAIPreview: Boolean = false,
    onShowAIPreviewChange: (Boolean) -> Unit = {}
) {
    LaunchedEffect(novelId) {
        viewModel.loadNovelId(novelId)
        aiChatViewModel.setNovelId(novelId)
    }
    
    val aiChatState by aiChatViewModel.uiState.collectAsState()
    val streamingMessage by aiChatViewModel.streamingMessage.collectAsState()
    val streamingExpanded by aiChatViewModel.streamingExpanded.collectAsState()
    val chatMessages = aiChatState.messages
    val isAiLoading = aiChatState.isLoading
    val aiError = aiChatState.error
    val chatSessions = aiChatState.sessions
    
    val messageOperations = remember { mutableStateMapOf<Long, List<com.bicy.novel.data.local.entity.AIOperationEntity>>() }
    
    LaunchedEffect(chatMessages) {
        chatMessages.forEach { message ->
            if (!message.isUser()) {
                val ops = aiChatViewModel.getOperationsForMessage(message.id)
                if (ops.isNotEmpty()) {
                    messageOperations[message.id] = ops
                }
            }
        }
    }
    
    var showUndoConfirmDialog by remember { mutableStateOf<Long?>(null) }
    var undoResultMessage by remember { mutableStateOf<String?>(null) }
    
    var showItemMenu by remember { mutableStateOf<QuickItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<QuickItem?>(null) }
    var showRenameDialog by remember { mutableStateOf<QuickItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createText by remember { mutableStateOf("") }
    val createScope = rememberCoroutineScope()
    
    if (showCreateDialog && section != QuickNavSection.AI) {
        val createLabel = when (section) {
            QuickNavSection.CONTENT -> "章节标题"
            QuickNavSection.CHARACTER -> "角色名称"
            QuickNavSection.WORLDVIEW -> "世界观标题"
            QuickNavSection.NOTE -> "笔记标题"
            QuickNavSection.TIMELINE -> "事件标题"
            QuickNavSection.AI -> ""
        }
        
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建${when (section) {
                QuickNavSection.CONTENT -> "章节"
                QuickNavSection.CHARACTER -> "角色"
                QuickNavSection.WORLDVIEW -> "世界观"
                QuickNavSection.NOTE -> "笔记"
                QuickNavSection.TIMELINE -> "时间线"
                QuickNavSection.AI -> ""
            }}") },
            text = {
                OutlinedTextField(
                    value = createText,
                    onValueChange = { createText = it },
                    label = { Text(createLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (createText.isNotBlank()) {
                            createScope.launch {
                                val newId = when (section) {
                                    QuickNavSection.CONTENT -> viewModel.createNewChapter(createText)
                                    QuickNavSection.CHARACTER -> viewModel.createNewCharacter(createText)
                                    QuickNavSection.WORLDVIEW -> viewModel.createNewWorldview(createText)
                                    QuickNavSection.NOTE -> viewModel.createNewNote(createText)
                                    QuickNavSection.TIMELINE -> viewModel.createNewTimeline(createText)
                                    QuickNavSection.AI -> 0L
                                }
                                when (section) {
                                    QuickNavSection.CONTENT -> onChapterClick(newId)
                                    QuickNavSection.CHARACTER -> onCharacterClick(newId)
                                    QuickNavSection.WORLDVIEW -> onWorldviewClick(newId)
                                    QuickNavSection.NOTE -> onNoteClick(newId)
                                    QuickNavSection.TIMELINE -> onTimelineClick(newId)
                                    QuickNavSection.AI -> {}
                                }
                            }
                            createText = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showDeleteConfirm != null) {
        val item = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${item.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(section, item.id)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showRenameDialog != null) {
        val item = showRenameDialog!!
        val chapters by viewModel.chapters.collectAsState()
        val sortedChapters = remember(chapters) { chapters.sortedBy { it.sortOrder } }
        val currentIndex = remember(sortedChapters, item) { sortedChapters.indexOfFirst { it.id == item.id } }
        val totalChapters = sortedChapters.size
        
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text(if (section == QuickNavSection.CONTENT) "编辑章节" else "重命名") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(if (section == QuickNavSection.CONTENT) "章节标题" else "名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (section == QuickNavSection.CONTENT && currentIndex >= 0) {
                        HorizontalDivider()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "第 ${currentIndex + 1} 章 / 共 $totalChapters 章",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.moveChapterUp(item.id)
                                    showRenameDialog = null
                                },
                                enabled = currentIndex > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("上移")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    viewModel.moveChapterDown(item.id)
                                    showRenameDialog = null
                                },
                                enabled = currentIndex < totalChapters - 1,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("下移")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameItem(section, item.id, renameText)
                        }
                        showRenameDialog = null
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showItemMenu != null) {
        val item = showItemMenu!!
        AlertDialog(
            onDismissRequest = { showItemMenu = null },
            title = { Text(item.name) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(if (section == QuickNavSection.CONTENT) "编辑" else "重命名") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.clickable {
                            renameText = item.name
                            showRenameDialog = item
                            showItemMenu = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text("添加至AI上下文") },
                        leadingContent = { Icon(Icons.Default.Psychology, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showItemMenu = null
                        }
                    )
                    ListItem(
                        headlineContent = { Text("删除") },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showDeleteConfirm = item
                            showItemMenu = null
                        },
                        colors = ListItemDefaults.colors(
                            headlineColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showItemMenu = null }) {
                    Text("关闭")
                }
            }
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        val drafts by viewModel.drafts.collectAsState()
        val draftMap = remember(drafts) {
            drafts.associateBy { "${it.targetType}_${it.targetId}" }
        }
        
        when (section) {
        QuickNavSection.CONTENT -> {
            val chapters by viewModel.chapters.collectAsState()
            val filteredChapters = remember(chapters, searchQuery) {
                if (searchQuery.isBlank()) chapters
                else chapters.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.content.contains(searchQuery, ignoreCase = true) 
                }
            }
            QuickItemList(
                items = filteredChapters.map { 
                    val draft = draftMap["chapter_${it.id}"]
                    QuickItem(it.id, it.title, draft?.wordCount ?: it.wordCount, it.sortOrder, isDraft = draft != null) 
                },
                onItemClick = onChapterClick,
                onItemLongPress = { showItemMenu = it },
                emptyText = if (searchQuery.isBlank()) "暂无章节" else "未找到匹配的章节"
            )
        }
        QuickNavSection.CHARACTER -> {
            val characters by viewModel.characters.collectAsState()
            val filteredCharacters = remember(characters, searchQuery) {
                if (searchQuery.isBlank()) characters
                else characters.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) || 
                    it.description.contains(searchQuery, ignoreCase = true) 
                }
            }
            QuickItemList(
                items = filteredCharacters.map { 
                    val draft = draftMap["character_${it.id}"]
                    QuickItem(it.id, it.name, draft?.wordCount ?: WordCounter.count(it.description), isDraft = draft != null) 
                },
                onItemClick = onCharacterClick,
                onItemLongPress = { showItemMenu = it },
                emptyText = if (searchQuery.isBlank()) "暂无角色" else "未找到匹配的角色"
            )
        }
        QuickNavSection.WORLDVIEW -> {
            val worldviews by viewModel.worldviews.collectAsState()
            val filteredWorldviews = remember(worldviews, searchQuery) {
                if (searchQuery.isBlank()) worldviews
                else worldviews.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.content.contains(searchQuery, ignoreCase = true) 
                }
            }
            QuickItemList(
                items = filteredWorldviews.map { 
                    val draft = draftMap["worldview_${it.id}"]
                    QuickItem(it.id, it.title, draft?.wordCount ?: WordCounter.count(it.content), isDraft = draft != null) 
                },
                onItemClick = onWorldviewClick,
                onItemLongPress = { showItemMenu = it },
                emptyText = if (searchQuery.isBlank()) "暂无世界观" else "未找到匹配的世界观"
            )
        }
        QuickNavSection.NOTE -> {
            val notes by viewModel.notes.collectAsState()
            val filteredNotes = remember(notes, searchQuery) {
                if (searchQuery.isBlank()) notes
                else notes.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.content.contains(searchQuery, ignoreCase = true) 
                }
            }
            QuickItemList(
                items = filteredNotes.map { 
                    val draft = draftMap["note_${it.id}"]
                    QuickItem(it.id, it.title, draft?.wordCount ?: WordCounter.count(it.content), isDraft = draft != null) 
                },
                onItemClick = onNoteClick,
                onItemLongPress = { showItemMenu = it },
                emptyText = if (searchQuery.isBlank()) "暂无笔记" else "未找到匹配的笔记"
            )
        }
        QuickNavSection.TIMELINE -> {
            val events by viewModel.timelineEvents.collectAsState()
            val filteredEvents = remember(events, searchQuery) {
                if (searchQuery.isBlank()) events
                else events.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.description.contains(searchQuery, ignoreCase = true) 
                }
            }
            QuickItemList(
                items = filteredEvents.map { 
                    val draft = draftMap["timeline_${it.id}"]
                    QuickItem(it.id, it.title, draft?.wordCount ?: WordCounter.count(it.description), isDraft = draft != null) 
                },
                onItemClick = onTimelineClick,
                onItemLongPress = { showItemMenu = it },
                emptyText = if (searchQuery.isBlank()) "暂无时间线" else "未找到匹配的时间线"
            )
        }
        QuickNavSection.AI -> {
            var showHistoryDialog by remember { mutableStateOf(false) }
            
            if (showHistoryDialog) {
                AlertDialog(
                    onDismissRequest = { showHistoryDialog = false },
                    title = { Text("对话历史") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (chatSessions.isEmpty()) {
                                Text(
                                    "暂无历史对话",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                chatSessions.forEach { session ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                aiChatViewModel.loadSession(session.id)
                                                showHistoryDialog = false
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            session.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { aiChatViewModel.deleteSession(session.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "删除",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHistoryDialog = false }) {
                            Text("关闭")
                        }
                    }
                )
            }
            
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("对话", style = MaterialTheme.typography.labelSmall)
                    Switch(
                        checked = aiMode == AIMode.AGENT,
                        onCheckedChange = { onAiModeChange(if (it) AIMode.AGENT else AIMode.CHAT) }
                    )
                    Text("Agent", style = MaterialTheme.typography.labelSmall)
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = { aiChatViewModel.startNewSession() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "新对话",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "历史对话",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                HorizontalDivider()
                
                val chatListState = rememberLazyListState()
                var isUserAtBottom by remember { mutableStateOf(true) }
                var lastMessageCount by remember { mutableStateOf(0) }
                val coroutineScope = rememberCoroutineScope()
                
                LaunchedEffect(chatListState.firstVisibleItemIndex) {
                    val totalItems = chatMessages.size + (if (streamingMessage != null) 1 else 0) + (if (isAiLoading && streamingMessage == null) 1 else 0)
                    val lastVisibleIndex = chatListState.firstVisibleItemIndex + chatListState.layoutInfo.visibleItemsInfo.size - 1
                    isUserAtBottom = lastVisibleIndex >= totalItems - 2 || totalItems <= 3
                }
                
                LaunchedEffect(chatMessages.size, streamingMessage) {
                    if (chatMessages.size > lastMessageCount || streamingMessage != null) {
                        if (isUserAtBottom || chatMessages.size > lastMessageCount + 1) {
                            if (chatMessages.isNotEmpty() || streamingMessage != null) {
                                coroutineScope.launch {
                                    chatListState.animateScrollToItem(
                                        index = if (streamingMessage != null) chatMessages.size + 1 else chatMessages.size
                                    )
                                }
                            }
                        }
                    }
                    lastMessageCount = chatMessages.size
                }
                
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (chatMessages.isEmpty() && streamingMessage == null) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("AI 助手", style = MaterialTheme.typography.titleMedium)
                                    Text("输入消息开始对话", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(chatMessages, key = { it.id }) { message ->
                                val operations = messageOperations[message.id] ?: emptyList()
                                val operationInfos = operations.map { op ->
                                    OperationInfo(
                                        operationType = op.operationType,
                                        targetType = op.targetType,
                                        targetName = op.targetName
                                    )
                                }
                                
                                AIMessageContent(
                                    content = message.content,
                                    isUser = message.isUser(),
                                    messageId = message.id,
                                    operations = operationInfos,
                                    canUndo = operations.isNotEmpty(),
                                    onUndo = { showUndoConfirmDialog = message.id }
                                )
                            }
                            
                            streamingMessage?.let { msg ->
                                item {
                                    StreamingMessageView(
                                        streamingMessage = msg
                                    )
                                }
                            }
                        }
                        
                        if (isAiLoading && streamingMessage == null) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI 正在思考...", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    
                    if (!isUserAtBottom && chatMessages.isNotEmpty()) {
                        FilledIconButton(
                            onClick = {
                                coroutineScope.launch {
                                    chatListState.animateScrollToItem(
                                        index = if (streamingMessage != null) chatMessages.size + 1 else chatMessages.size
                                    )
                                    isUserAtBottom = true
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "滚动到底部",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                if (aiError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                aiError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { aiChatViewModel.clearError() }
                            ) {
                                Text("关闭", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                HorizontalDivider()
                
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    if (!selectedText.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "已选中文字",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        selectedText.take(50) + if (selectedText.length > 50) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        maxLines = 2
                                    )
                                }
                                IconButton(
                                    onClick = { onSelectedTextChange(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "取消选择",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    OutlinedTextField(
                        value = aiInput,
                        onValueChange = onAiInputChange,
                        placeholder = { Text(if (selectedText.isNullOrBlank()) "输入消息..." else "输入指令处理选中文字...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val chapters by viewModel.chapters.collectAsState()
                    val chaptersInfo = remember(chapters) {
                        chapters.map { com.bicy.novel.data.ai.ChapterSummary(it.id, it.title, it.wordCount, it.sortOrder) }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isAiLoading) {
                            Button(
                                onClick = { aiChatViewModel.stopGeneration() },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("停止")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (aiInput.isNotBlank()) {
                                        val context = com.bicy.novel.data.ai.AIContext(
                                            novelTitle = novelTitle,
                                            novelId = novelId,
                                            currentChapterId = currentChapterId,
                                            currentChapterTitle = currentChapterTitle,
                                            currentChapter = currentContent,
                                            chaptersSummary = chaptersInfo
                                        )
                                        aiChatViewModel.sendMessage(aiInput, context, isAgent = aiMode == AIMode.AGENT)
                                        onAiInputChange("")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = aiInput.isNotBlank()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("发送")
                            }
                        }
                    }
                }
            }
        }
    }
        
        if (section != QuickNavSection.AI) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建")
            }
        }
    }
    
    showUndoConfirmDialog?.let { messageId ->
        AlertDialog(
            onDismissRequest = { showUndoConfirmDialog = null },
            title = { Text("撤销AI操作") },
            text = { 
                val ops = messageOperations[messageId] ?: emptyList()
                if (ops.isNotEmpty()) {
                    Column {
                        Text("将撤销以下操作：")
                        Spacer(modifier = Modifier.height(8.dp))
                        ops.forEach { op ->
                            val opName = when (op.operationType) {
                                "CREATE" -> "创建"
                                "UPDATE" -> "编辑"
                                "DELETE" -> "删除"
                                else -> op.operationType
                            }
                            val targetName = when (op.targetType) {
                                "chapter" -> "章节"
                                "character" -> "角色"
                                "worldview" -> "世界观"
                                "note" -> "笔记"
                                "timeline" -> "时间线"
                                else -> op.targetType
                            }
                            Text("• $opName $targetName「${op.targetName}」", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Text("该消息没有可撤销的操作")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        aiChatViewModel.undoMessage(
                            messageId = messageId,
                            onSuccess = { msg -> undoResultMessage = msg },
                            onError = { msg -> undoResultMessage = msg }
                        )
                        showUndoConfirmDialog = null
                    }
                ) {
                    Text("撤销")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoConfirmDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    undoResultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { undoResultMessage = null },
            title = { Text("撤销结果") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { undoResultMessage = null }) {
                    Text("确定")
                }
            }
        )
    }
}

data class QuickItem(val id: Long, val name: String, val wordCount: Int?, val sortOrder: Int = 0, val isDraft: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickItemList(
    items: List<QuickItem>,
    onItemClick: (Long) -> Unit,
    onItemLongPress: ((QuickItem) -> Unit)? = null,
    emptyText: String
) {
    if (items.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onItemClick(item.id) },
                            onLongClick = { onItemLongPress?.invoke(item) }
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.weight(1f))
                        if (item.wordCount != null && item.wordCount > 0) {
                            Surface(
                                color = if (item.isDraft) MaterialTheme.colorScheme.errorContainer 
                                        else MaterialTheme.colorScheme.primaryContainer, 
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    WordCounter.formatWordCount(item.wordCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.isDraft) MaterialTheme.colorScheme.error 
                                            else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
