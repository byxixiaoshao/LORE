package com.bicy.novel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class NovelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val description: String = "",
    val coverPath: String? = null,
    val category: String = "",
    val status: Int = 0,
    val totalWords: Int = 0,
    val chapterCount: Int = 0,
    val isLocked: Boolean = false, // 项目是否被锁定
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
