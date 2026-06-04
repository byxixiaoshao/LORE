package com.bicy.novel.data.export

import com.bicy.novel.domain.model.*
import kotlinx.serialization.Serializable

@Serializable
data class NovelExportData(
    val version: Int = 1,
    val exportTime: Long = System.currentTimeMillis(),
    val novel: NovelData,
    val chapters: List<ChapterData> = emptyList(),
    val characters: List<CharacterData> = emptyList(),
    val worldviews: List<WorldviewData> = emptyList(),
    val notes: List<NoteData> = emptyList(),
    val timelineEvents: List<TimelineEventData> = emptyList(),
    val contentHistories: List<ContentHistoryData> = emptyList()
)

@Serializable
data class NovelData(
    val title: String,
    val author: String,
    val description: String,
    val coverPath: String? = null,
    val category: String,
    val status: Int,
    val totalWords: Int,
    val chapterCount: Int,
    val lastEditedType: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ChapterData(
    val title: String,
    val content: String,
    val wordCount: Int,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CharacterData(
    val name: String,
    val alias: String,
    val roleType: String,
    val description: String,
    val avatarPath: String? = null,
    val attributes: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class WorldviewData(
    val title: String,
    val category: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class NoteData(
    val title: String,
    val content: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class TimelineEventData(
    val title: String,
    val description: String,
    val eventDate: String,
    val sortOrder: Int,
    val isKeyEvent: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ContentHistoryData(
    val targetType: String,
    val targetId: Long,
    val title: String,
    val content: String,
    val wordCount: Int,
    val savedAt: Long,
    val note: String? = null
)

fun Novel.toExportData() = NovelData(
    title = title,
    author = author,
    description = description,
    coverPath = coverPath,
    category = category,
    status = status,
    totalWords = totalWords,
    chapterCount = chapterCount,
    lastEditedType = lastEditedType,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Chapter.toExportData() = ChapterData(
    title = title,
    content = content,
    wordCount = wordCount,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Character.toExportData() = CharacterData(
    name = name,
    alias = alias,
    roleType = roleType,
    description = description,
    avatarPath = avatarPath,
    attributes = attributes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Worldview.toExportData() = WorldviewData(
    title = title,
    category = category,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Note.toExportData() = NoteData(
    title = title,
    content = content,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TimelineEvent.toExportData() = TimelineEventData(
    title = title,
    description = description,
    eventDate = eventDate,
    sortOrder = sortOrder,
    isKeyEvent = isKeyEvent,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ContentHistory.toExportData() = ContentHistoryData(
    targetType = targetType,
    targetId = targetId,
    title = title,
    content = content,
    wordCount = wordCount,
    savedAt = savedAt,
    note = note
)
