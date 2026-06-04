package com.bicy.novel.domain.repository

import com.bicy.novel.domain.model.Novel
import kotlinx.coroutines.flow.Flow

interface NovelRepository {
    fun getAllNovels(): Flow<List<Novel>>
    suspend fun getNovelById(id: Long): Novel?
    fun getNovelByIdFlow(id: Long): Flow<Novel?>
    fun searchNovels(keyword: String): Flow<List<Novel>>
    suspend fun createNovel(novel: Novel): Long
    suspend fun updateNovel(novel: Novel)
    suspend fun deleteNovel(id: Long)
    suspend fun updateNovelStats(id: Long, wordCount: Int, chapterCount: Int)
    suspend fun getTotalWords(): Int
    suspend fun updateNovelLock(id: Long, isLocked: Boolean)
}
