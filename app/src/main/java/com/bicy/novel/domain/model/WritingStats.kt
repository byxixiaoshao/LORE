package com.bicy.novel.domain.model

data class WritingStats(
    val id: Long = 0,
    val novelId: Long,
    val date: Long,
    val wordCount: Int,
    val wordsWritten: Int,
    val chaptersWritten: Int,
    val editDuration: Long,
    val createdAt: Long
)

fun WritingStats.toEntity() = com.bicy.novel.data.local.entity.WritingStatsEntity(
    id = id,
    novelId = novelId,
    date = date,
    wordCount = wordCount,
    wordsWritten = wordsWritten,
    chaptersWritten = chaptersWritten,
    editDuration = editDuration,
    createdAt = createdAt
)

fun com.bicy.novel.data.local.entity.WritingStatsEntity.toModel() = WritingStats(
    id = id,
    novelId = novelId,
    date = date,
    wordCount = wordCount,
    wordsWritten = wordsWritten,
    chaptersWritten = chaptersWritten,
    editDuration = editDuration,
    createdAt = createdAt
)
