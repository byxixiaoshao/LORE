package com.bicy.novel.data.repository

import com.bicy.novel.data.local.dao.ChapterDao
import com.bicy.novel.data.local.entity.ChapterEntity
import com.bicy.novel.domain.model.Chapter
import com.bicy.novel.domain.repository.ChapterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepositoryImpl @Inject constructor(
    private val chapterDao: ChapterDao
) : ChapterRepository {
    
    override fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>> {
        return chapterDao.getChaptersByNovelId(novelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getChaptersByNovelIdOnce(novelId: Long): List<Chapter> {
        return chapterDao.getChaptersByNovelIdOnce(novelId).map { it.toDomain() }
    }
    
    override suspend fun getChapterById(id: Long): Chapter? {
        return chapterDao.getChapterById(id)?.toDomain()
    }
    
    override fun getChapterByIdFlow(id: Long): Flow<Chapter?> {
        return chapterDao.getChapterByIdFlow(id).map { it?.toDomain() }
    }
    
    override suspend fun createChapter(chapter: Chapter): Long {
        val maxOrder = chapterDao.getMaxSortOrder(chapter.novelId) ?: -1
        return chapterDao.insertChapter(chapter.toEntity().copy(sortOrder = maxOrder + 1))
    }
    
    override suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(chapter.toEntity())
    }
    
    override suspend fun deleteChapter(id: Long) {
        chapterDao.softDeleteChapter(id)
    }
    
    override suspend fun getTotalWordCount(novelId: Long): Int {
        return chapterDao.getTotalWordCount(novelId) ?: 0
    }
    
    override suspend fun getChapterCount(novelId: Long): Int {
        return chapterDao.getChapterCount(novelId)
    }
    
    override suspend fun getFirstChapter(novelId: Long): Chapter? {
        return chapterDao.getFirstChapter(novelId)?.toDomain()
    }
    
    override suspend fun getNextChapter(novelId: Long, currentSortOrder: Int): Chapter? {
        return chapterDao.getNextChapter(novelId, currentSortOrder)?.toDomain()
    }
    
    override suspend fun getPreviousChapter(novelId: Long, currentSortOrder: Int): Chapter? {
        return chapterDao.getPreviousChapter(novelId, currentSortOrder)?.toDomain()
    }
    
    private fun ChapterEntity.toDomain() = Chapter(
        id = id,
        novelId = novelId,
        volumeId = volumeId,
        title = title,
        content = content,
        wordCount = wordCount,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun Chapter.toEntity() = ChapterEntity(
        id = id,
        novelId = novelId,
        volumeId = volumeId,
        title = title,
        content = content,
        wordCount = wordCount,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
