package com.bicy.novel.domain.repository

import com.bicy.novel.domain.model.WritingStats
import kotlinx.coroutines.flow.Flow

/**
 * 写作统计仓库接口
 */
interface WritingStatsRepository {
    /**
     * 获取指定小说的所有统计数据
     */
    fun getStatsByNovel(novelId: Long): Flow<List<WritingStats>>
    
    /**
     * 获取指定日期的统计数据
     */
    suspend fun getStatsByDate(novelId: Long, date: Long): WritingStats?
    
    /**
     * 获取指定日期范围内的统计数据
     */
    suspend fun getStatsInRange(novelId: Long, startDate: Long, endDate: Long): List<WritingStats>
    
    /**
     * 记录写作统计
     * @param novelId 小说ID
     * @param wordCount 当前总字数
     * @param wordsWritten 本次新增字数
     * @param chaptersWritten 本次编辑章节数
     * @param editDuration 编辑时长（毫秒）
     */
    suspend fun recordStats(
        novelId: Long,
        wordCount: Int = 0,
        wordsWritten: Int = 0,
        chaptersWritten: Int = 0,
        editDuration: Long = 0
    )
    
    /**
     * 获取总创作字数
     */
    suspend fun getTotalWordsWritten(novelId: Long): Int
    
    /**
     * 获取总编辑时长（毫秒）
     */
    suspend fun getTotalEditDuration(novelId: Long): Long
    
    /**
     * 获取平均每日字数
     */
    suspend fun getAverageDailyWords(novelId: Long): Double
    
    /**
     * 删除指定小说的所有统计数据
     */
    suspend fun deleteStatsByNovel(novelId: Long)
}
