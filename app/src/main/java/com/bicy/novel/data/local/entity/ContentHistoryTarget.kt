package com.bicy.novel.data.local.entity

import androidx.room.ColumnInfo

data class ContentHistoryTarget(
    @ColumnInfo(name = "targetType")
    val targetType: String,
    @ColumnInfo(name = "targetId")
    val targetId: Long
)
