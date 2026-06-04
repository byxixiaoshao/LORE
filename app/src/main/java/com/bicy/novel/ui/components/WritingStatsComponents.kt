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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bicy.novel.domain.model.WritingStats
import java.text.SimpleDateFormat
import java.util.*

/**
 * 创作统计卡片
 */
@Composable
fun WritingStatsCard(
    stats: List<WritingStats>,
    totalWords: Int,
    modifier: Modifier = Modifier
) {
    val totalWordsWritten = stats.sumOf { it.wordsWritten }
    val totalEditDuration = stats.sumOf { it.editDuration }
    val avgDailyWords = if (stats.isNotEmpty()) totalWordsWritten / stats.size else 0
    val totalChaptersEdited = stats.sumOf { it.chaptersWritten }
    
    // 计算编辑时长（小时）
    val editHours = totalEditDuration / (1000 * 60 * 60)
    val editMinutes = (totalEditDuration / (1000 * 60)) % 60
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "创作统计",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = "统计",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 统计数据网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                @Suppress("DEPRECATION")
                StatItem(
                    icon = Icons.Default.Article,
                    label = "总字数",
                    value = formatNumber(totalWords),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = Icons.Default.Edit,
                    label = "创作字数",
                    value = formatNumber(totalWordsWritten),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatItem(
                    icon = Icons.Default.Today,
                    label = "日均字数",
                    value = formatNumber(avgDailyWords),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = Icons.Default.Schedule,
                    label = "创作时长",
                    value = "${editHours}h ${editMinutes}m",
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 字数趋势图
            if (stats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "字数趋势（最近7天）",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                WordTrendChart(
                    stats = stats.takeLast(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    }
}

/**
 * 单个统计项
 */
@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 字数趋势图（简单柱状图）
 */
@Composable
private fun WordTrendChart(
    stats: List<WritingStats>,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val maxWords = stats.maxOf { it.wordsWritten }.coerceAtLeast(1)
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEach { stat ->
            val heightFraction = stat.wordsWritten.toFloat() / maxWords
            val dateLabel = dateFormat.format(Date(stat.date))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // 柱状条
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height((heightFraction * 80).toInt().dp)
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
                
                // 日期标签
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> String.format("%.1f万", num / 10000.0)
        num >= 1000 -> String.format("%.1fk", num / 1000.0)
        else -> num.toString()
    }
}
