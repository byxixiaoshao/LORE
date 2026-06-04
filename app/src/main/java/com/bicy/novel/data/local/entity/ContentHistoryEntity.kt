package com.bicy.novel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "content_history",
    indices = [
        androidx.room.Index(value = ["targetId", "targetType"]),
        androidx.room.Index(value = ["savedAt"])
    ]
)
data class ContentHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetType: String, // "chapter", "character", "worldview", "note", "timeline"
    val targetId: Long,
    val title: String,
    val content: String,
    val wordCount: Int,
    val savedAt: Long = System.currentTimeMillis(),
    val note: String? = null  // 可选的版本说明
)
