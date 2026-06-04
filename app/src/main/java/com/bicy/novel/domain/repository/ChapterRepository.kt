package com.bicy.novel.domain.repository

import com.bicy.novel.domain.model.Chapter
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>>
    suspend fun getChaptersByNovelIdOnce(novelId: Long): List<Chapter>
    suspend fun getChapterById(id: Long): Chapter?
    fun getChapterByIdFlow(id: Long): Flow<Chapter?>
    suspend fun createChapter(chapter: Chapter): Long
    suspend fun updateChapter(chapter: Chapter)
    suspend fun deleteChapter(id: Long)
    suspend fun getTotalWordCount(novelId: Long): Int
    suspend fun getChapterCount(novelId: Long): Int
    suspend fun getFirstChapter(novelId: Long): Chapter?
    suspend fun getNextChapter(novelId: Long, currentSortOrder: Int): Chapter?
    suspend fun getPreviousChapter(novelId: Long, currentSortOrder: Int): Chapter?
}
