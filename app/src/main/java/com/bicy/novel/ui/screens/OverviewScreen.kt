package com.bicy.novel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bicy.novel.ui.components.LoadingState
import com.bicy.novel.ui.components.SecurityLockInputDialog
import com.bicy.novel.ui.components.SecurityQuestionDialog
import com.bicy.novel.util.DateTimeUtils
import com.bicy.novel.util.WordCounter

@Composable
fun OverviewScreen(
    novelId: Long,
    onStartEditClick: (Long, String) -> Unit,
    onEditNovelClick: () -> Unit,
    onShowNewChapterDialog: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(novelId) {
        viewModel.loadNovel(novelId)
    }
    
    val novel by viewModel.novel.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val recentStats by viewModel.recentStats.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    
    // 安全锁相关状态
    var showLockVerifyDialog by remember { mutableStateOf(false) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var lockVerifyPurpose by remember { mutableStateOf("") } // lock/unlock/export
    var verifyFailedCount by remember { mutableStateOf(0) }
    
    if (isLoading) {
        LoadingState(modifier = modifier)
        return
    }
    
    // 密码验证对话框
    if (showLockVerifyDialog && settings.isSecurityLockSet) {
        SecurityLockInputDialog(
            title = "验证安全锁",
            subtitle = "请输入密码",
            isRegisterMode = false,
            onConfirm = { password ->
                if (settingsViewModel.verifySecurityLockPassword(password)) {
                    showLockVerifyDialog = false
                    verifyFailedCount = 0
                    when (lockVerifyPurpose) {
                        "lock" -> {
                            viewModel.updateNovelLock(novelId, true)
                        }
                        "unlock" -> {
                            viewModel.updateNovelLock(novelId, false)
                        }
                        "export" -> {
                            onExportClick()
                        }
                    }
                } else {
                    verifyFailedCount++
                    if (verifyFailedCount >= 5) {
                        showLockVerifyDialog = false
                        showQuestionDialog = true
                    }
                }
            },
            onDismiss = { showLockVerifyDialog = false },
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
                when (lockVerifyPurpose) {
                    "lock" -> {
                        viewModel.updateNovelLock(novelId, true)
                    }
                    "unlock" -> {
                        viewModel.updateNovelLock(novelId, false)
                    }
                    "export" -> {
                        onExportClick()
                    }
                }
            },
            onDismiss = { showQuestionDialog = false },
            onVerifyAnswer = { answer -> settingsViewModel.verifySecurityAnswer(answer) }
        )
    }
    
    novel?.let { currentNovel ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentNovel.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (currentNovel.author.isNotEmpty()) {
                        Text(
                            text = "作者：${currentNovel.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (currentNovel.category.isNotEmpty()) {
                        Text(
                            text = "分类：${currentNovel.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.determineEditTarget { target ->
                            if (target != null) {
                                onStartEditClick(target.id, target.type)
                            } else {
                                onShowNewChapterDialog()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始编辑")
                    }
                    
                    // 锁按钮
                    if (settings.isSecurityLockSet) {
                        IconButton(onClick = {
                            if (currentNovel.isLocked) {
                                lockVerifyPurpose = "unlock"
                            } else {
                                lockVerifyPurpose = "lock"
                            }
                            showLockVerifyDialog = true
                        }) {
                            Icon(
                                if (currentNovel.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (currentNovel.isLocked) "解锁" else "锁定"
                            )
                        }
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "简介",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentNovel.description.isNotEmpty()) currentNovel.description else "暂无简介",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentNovel.description.isNotEmpty()) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "统计信息",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StatRow("创作时长", DateTimeUtils.formatTime(currentNovel.createdAt))
                    StatRow("创作字数", WordCounter.formatWordCount(stats.wordCount))
                    StatRow("笔记数量", "${stats.noteCount} 条")
                    StatRow("章节数量", "${stats.chapterCount} 章")
                    StatRow("角色数量", "${stats.characterCount} 个")
                    StatRow("时间线事件", "${stats.timelineEventCount} 个")
                    
                    if (stats.totalWritingDuration > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        StatRow("累计写作时长", formatDuration(stats.totalWritingDuration))
                        StatRow("累计编辑次数", "${stats.totalEditCount} 次")
                    }
                }
            }
            
            // 字数趋势图表
            if (recentStats.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "字数趋势（最近7天）",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        WordCountChart(
                            stats = recentStats,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
            
            // 创作习惯统计
            if (recentStats.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "创作习惯",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val totalWords = recentStats.sumOf { it.wordsWritten }
                        val avgWords = if (recentStats.isNotEmpty()) totalWords / recentStats.size else 0
                        val activeDays = recentStats.count { it.wordsWritten != 0 }
                        
                        StatRow("7天总字数", WordCounter.formatWordCount(totalWords))
                        StatRow("日均字数", WordCounter.formatWordCount(avgWords))
                        StatRow("活跃天数", "$activeDays 天")
                        
                        // 显示每日详情
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "每日详情",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        recentStats.sortedByDescending { it.date }.forEach { stat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = DateTimeUtils.formatDate(stat.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (stat.wordsWritten >= 0) "+${stat.wordsWritten}" else "${stat.wordsWritten}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (stat.wordsWritten >= 0) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditNovelClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("编辑信息")
                }
                
                OutlinedButton(
                    onClick = {
                        if (currentNovel.isLocked && settings.isSecurityLockSet) {
                            lockVerifyPurpose = "export"
                            showLockVerifyDialog = true
                        } else {
                            onExportClick()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("导出项目")
                }
            }
            
            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("删除项目")
            }
        }
    }
}

/**
 * 字数趋势图表组件
 */
@Composable
private fun WordCountChart(
    stats: List<com.bicy.novel.domain.model.WritingStats>,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val sortedStats = stats.sortedBy { it.date }
    val maxWords = (sortedStats.maxOfOrNull { it.wordsWritten }.takeIf { it != null && it > 0 } ?: 1).toFloat()
    val minWords = (sortedStats.minOfOrNull { it.wordsWritten } ?: 0).toFloat()
    val range = (maxWords - minWords).takeIf { it > 0 } ?: 1f
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        modifier = modifier.drawBehind {
            // 绘制网格线
            val gridLines = 5
            for (i in 0..gridLines) {
                val y = size.height * i / gridLines
                drawLine(
                    color = surfaceVariantColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
            
            // 绘制折线图
            if (sortedStats.size > 1) {
                val points = sortedStats.mapIndexed { index, stat ->
                    val x = size.width * index / (sortedStats.size - 1)
                    val y = size.height * (1 - (stat.wordsWritten - minWords) / range)
                    Offset(x, y)
                }
                
                // 绘制连线
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = primaryColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3f
                    )
                }
                
                // 绘制数据点
                points.forEach { point ->
                    drawCircle(
                        color = primaryColor,
                        radius = 6f,
                        center = point
                    )
                }
            }
        }
    ) {
        // 显示日期标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            sortedStats.forEach { stat ->
                Text(
                    text = DateTimeUtils.formatDateShort(stat.date), // 只显示月-日
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 格式化时长（毫秒转为可读格式）
 */
private fun formatDuration(milliseconds: Long): String {
    val minutes = (milliseconds / (1000 * 60)).toInt()
    return when {
        minutes < 60 -> "${minutes}分钟"
        minutes < 1440 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "${hours}小时${mins}分钟" else "${hours}小时"
        }
        else -> {
            val days = minutes / 1440
            val hours = (minutes % 1440) / 60
            if (hours > 0) "${days}天${hours}小时" else "${days}天"
        }
    }
}
