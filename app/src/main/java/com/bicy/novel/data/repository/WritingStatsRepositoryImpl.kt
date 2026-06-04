package com.bicy.novel.data.repository

import com.bicy.novel.data.local.dao.WritingStatsDao
import com.bicy.novel.data.local.entity.WritingStatsEntity
import com.bicy.novel.domain.model.WritingStats
import com.bicy.novel.domain.repository.WritingStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 写作统计仓库实现
 */
@Singleton
class WritingStatsRepositoryImpl @Inject constructor(
    private val writingStatsDao: WritingStatsDao
) : WritingStatsRepository {
    
    override fun getStatsByNovel(novelId: Long): Flow<List<WritingStats>> {
        return writingStatsDao.getStatsByNovel(novelId).map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    override suspend fun getStatsInRange(novelId: Long, startDate: Long, endDate: Long): List<WritingStats> {
        return writingStatsDao.getStatsInRange(novelId, startDate, endDate).map { it.toModel() }
    }
    
    override suspend fun getStatsByDate(novelId: Long, date: Long): WritingStats? {
        return writingStatsDao.getStatsByDate(novelId, date)?.toModel()
    }
    
    override suspend fun recordStats(
        novelId: Long,
        wordCount: Int,
        wordsWritten: Int,
        chaptersWritten: Int,
        editDuration: Long
    ) {
        val today = getTodayTimestamp()
        writingStatsDao.updateOrInsert(
            novelId = novelId,
            date = today,
            wordCount = wordCount,
            wordsWritten = wordsWritten,
            chaptersWritten = chaptersWritten,
            editDuration = editDuration
        )
    }
    
    override suspend fun getTotalWordsWritten(novelId: Long): Int {
        return writingStatsDao.getTotalWordsWritten(novelId) ?: 0
    }
    
    override suspend fun getTotalEditDuration(novelId: Long): Long {
        return writingStatsDao.getTotalEditDuration(novelId) ?: 0L
    }
    
    override suspend fun getAverageDailyWords(novelId: Long): Double {
        return writingStatsDao.getAverageDailyWords(novelId) ?: 0.0
    }
    
    override suspend fun deleteStatsByNovel(novelId: Long) {
        writingStatsDao.deleteAllByNovel(novelId)
    }
    
    /**
     * 获取今天的时间戳（精确到天）
     */
    private fun getTodayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * 实体转领域模型
     */
    private fun WritingStatsEntity.toModel() = WritingStats(
        id = id,
        novelId = novelId,
        date = date,
        wordCount = wordCount,
        wordsWritten = wordsWritten,
        chaptersWritten = chaptersWritten,
        editDuration = editDuration,
        createdAt = createdAt
    )
}
