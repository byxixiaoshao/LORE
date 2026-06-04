package com.bicy.novel.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "drafts",
    primaryKeys = ["targetType", "targetId"],
    indices = [
        Index(value = ["novelId"])
    ]
)
data class DraftEntity(
    val targetType: String,      // "chapter", "character", "worldview", "note", "timeline"
    val targetId: Long,          // 0 = new unsaved item
    val novelId: Long,
    val title: String,
    val content: String,
    val wordCount: Int,
    val savedAt: Long = System.currentTimeMillis()
)
