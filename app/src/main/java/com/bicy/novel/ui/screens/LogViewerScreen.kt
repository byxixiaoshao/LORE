package com.bicy.novel.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    logType: String? = null,
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val appLogContent by viewModel.appLogContent.collectAsState()
    val crashLogContent by viewModel.crashLogContent.collectAsState()
    val logFiles by viewModel.logFiles.collectAsState()
    val selectedLogContent by viewModel.selectedLogContent.collectAsState()
    val selectedLogTitle by viewModel.selectedLogTitle.collectAsState()
    val context = LocalContext.current

    var showClearDialog by remember { mutableStateOf(false) }
    var viewingLog by remember { mutableStateOf(logType ?: "app") }

    LaunchedEffect(viewingLog) {
        when (viewingLog) {
            "app" -> viewModel.loadAppLog()
            "crash" -> viewModel.loadCrashLog()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadLogFiles()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("日志查看") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (viewingLog != "files") {
                        IconButton(onClick = {
                            val content = when (viewingLog) {
                                "app" -> appLogContent
                                "crash" -> crashLogContent
                                else -> selectedLogContent
                            }
                            if (content.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("log", content))
                            }
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                        }
                        IconButton(onClick = {
                            val content = when (viewingLog) {
                                "app" -> appLogContent
                                "crash" -> crashLogContent
                                else -> selectedLogContent
                            }
                            if (content.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, content)
                                }
                                context.startActivity(Intent.createChooser(intent, "导出日志"))
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "导出")
                        }
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "清空")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = when (viewingLog) {
                    "app" -> 0
                    "crash" -> 1
                    "files" -> 2
                    else -> 2
                },
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = viewingLog == "app",
                    onClick = { viewingLog = "app" },
                    text = { Text("应用日志") }
                )
                Tab(
                    selected = viewingLog == "crash",
                    onClick = { viewingLog = "crash" },
                    text = { Text("崩溃日志") }
                )
                Tab(
                    selected = viewingLog == "files",
                    onClick = {
                        viewingLog = "files"
                        viewModel.loadLogFiles()
                    },
                    text = { Text("日志文件") }
                )
            }

            when (viewingLog) {
                "app" -> LogContentViewer(
                    content = appLogContent,
                    emptyMessage = "暂无应用日志",
                    modifier = Modifier.fillMaxSize()
                )
                "crash" -> LogContentViewer(
                    content = crashLogContent,
                    emptyMessage = "暂无崩溃日志",
                    modifier = Modifier.fillMaxSize()
                )
                "files" -> LogFilesList(
                    files = logFiles,
                    onFileClick = { file ->
                        viewingLog = file.name
                        viewModel.loadLogFile(file)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                else -> LogContentViewer(
                    content = selectedLogContent,
                    emptyMessage = "暂无内容",
                    title = selectedLogTitle,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空") },
            text = {
                Text(
                    when (viewingLog) {
                        "app" -> "确定要清空应用日志吗？"
                        "crash" -> "确定要清空崩溃日志吗？"
                        else -> "确定要清空此日志吗？"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (viewingLog) {
                            "app" -> viewModel.clearAppLog()
                            "crash" -> viewModel.clearCrashLog()
                        }
                        showClearDialog = false
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun LogContentViewer(
    content: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    Box(modifier = modifier) {
        if (content.isBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val scrollStateH = rememberScrollState()
            val scrollStateV = rememberScrollState()

            Text(
                text = if (title != null) "$title\n\n$content" else content,
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollStateH)
                    .verticalScroll(scrollStateV)
                    .padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LogFilesList(
    files: List<File>,
    onFileClick: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    if (files.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无日志文件",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onFileClick(file) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))}  ${formatFileSize(file.length())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "%.1f MB".format(size / (1024.0 * 1024.0))
    }
}
