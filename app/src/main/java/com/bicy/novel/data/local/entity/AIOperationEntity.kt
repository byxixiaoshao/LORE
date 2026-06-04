package com.bicy.novel.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_operations",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["messageId"])
    ]
)
data class AIOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val messageId: Long,
    val operationType: String,
    val targetType: String,
    val targetId: Long,
    val targetName: String,
    val beforeData: String?,
    val afterData: String?,
    val timestamp: Long = System.currentTimeMillis()
)
