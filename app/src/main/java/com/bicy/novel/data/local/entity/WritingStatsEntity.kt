package com.bicy.novel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 创作统计实体
 * 记录每日的创作数据
 */
@Entity(
    tableName = "writing_stats",
    indices = [
        androidx.room.Index(value = ["novelId", "date"], unique = true)
    ]
)
data class WritingStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val novelId: Long,
    val date: Long, // 时间戳（精确到天）
    val wordCount: Int, // 当日总字数
    val wordsWritten: Int, // 当日新增字数
    val chaptersWritten: Int, // 当日编辑章节数
    val editDuration: Long, // 编辑时长（毫秒）
    val createdAt: Long = System.currentTimeMillis()
)
