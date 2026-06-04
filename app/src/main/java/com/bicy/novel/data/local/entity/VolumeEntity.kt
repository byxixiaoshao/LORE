package com.bicy.novel.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "volumes",
    foreignKeys = [
        ForeignKey(
            entity = NovelEntity::class,
            parentColumns = ["id"],
            childColumns = ["novelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["novelId"])]
)
data class VolumeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val novelId: Long,
    val title: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
