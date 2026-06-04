package com.bicy.novel.data.repository

import com.bicy.novel.data.local.dao.NovelDao
import com.bicy.novel.data.local.entity.NovelEntity
import com.bicy.novel.domain.model.Novel
import com.bicy.novel.domain.repository.NovelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovelRepositoryImpl @Inject constructor(
    private val novelDao: NovelDao
) : NovelRepository {
    
    override fun getAllNovels(): Flow<List<Novel>> {
        return novelDao.getAllNovels().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getNovelById(id: Long): Novel? {
        return novelDao.getNovelById(id)?.toDomain()
    }
    
    override fun getNovelByIdFlow(id: Long): Flow<Novel?> {
        return novelDao.getNovelByIdFlow(id).map { it?.toDomain() }
    }
    
    override fun searchNovels(keyword: String): Flow<List<Novel>> {
        return novelDao.searchNovels(keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun createNovel(novel: Novel): Long {
        return novelDao.insertNovel(novel.toEntity())
    }
    
    override suspend fun updateNovel(novel: Novel) {
        novelDao.updateNovel(novel.toEntity())
    }
    
    override suspend fun deleteNovel(id: Long) {
        novelDao.softDeleteNovel(id)
    }
    
    override suspend fun updateNovelStats(id: Long, wordCount: Int, chapterCount: Int) {
        novelDao.updateNovelStats(id, wordCount, chapterCount)
    }
    
    override suspend fun getTotalWords(): Int {
        return novelDao.getTotalWords() ?: 0
    }
    
    override suspend fun updateNovelLock(id: Long, isLocked: Boolean) {
        novelDao.updateNovelLock(id, isLocked)
    }
    
    private fun NovelEntity.toDomain() = Novel(
        id = id,
        title = title,
        author = author,
        description = description,
        coverPath = coverPath,
        category = category,
        status = status,
        totalWords = totalWords,
        chapterCount = chapterCount,
        isLocked = isLocked,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun Novel.toEntity() = NovelEntity(
        id = id,
        title = title,
        author = author,
        description = description,
        coverPath = coverPath,
        category = category,
        status = status,
        totalWords = totalWords,
        chapterCount = chapterCount,
        isLocked = isLocked,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
