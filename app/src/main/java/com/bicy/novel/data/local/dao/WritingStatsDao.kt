package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.WritingStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WritingStatsDao {
    
    @Query("SELECT * FROM writing_stats WHERE novelId = :novelId ORDER BY date DESC")
    fun getStatsByNovel(novelId: Long): Flow<List<WritingStatsEntity>>
    
    @Query("SELECT * FROM writing_stats WHERE novelId = :novelId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getStatsInRange(novelId: Long, startDate: Long, endDate: Long): List<WritingStatsEntity>
    
    @Query("SELECT * FROM writing_stats WHERE novelId = :novelId AND date = :date LIMIT 1")
    suspend fun getStatsByDate(novelId: Long, date: Long): WritingStatsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: WritingStatsEntity): Long
    
    @Update
    suspend fun update(stats: WritingStatsEntity)
    
    @Query("DELETE FROM writing_stats WHERE novelId = :novelId")
    suspend fun deleteAllByNovel(novelId: Long)
    
    @Query("SELECT SUM(wordsWritten) FROM writing_stats WHERE novelId = :novelId")
    suspend fun getTotalWordsWritten(novelId: Long): Int?
    
    @Query("SELECT SUM(editDuration) FROM writing_stats WHERE novelId = :novelId")
    suspend fun getTotalEditDuration(novelId: Long): Long?
    
    @Query("SELECT AVG(wordsWritten) FROM writing_stats WHERE novelId = :novelId")
    suspend fun getAverageDailyWords(novelId: Long): Double?
    
    @Transaction
    suspend fun updateOrInsert(
        novelId: Long,
        date: Long,
        wordCount: Int,
        wordsWritten: Int,
        chaptersWritten: Int,
        editDuration: Long
    ) {
        val existing = getStatsByDate(novelId, date)
        if (existing != null) {
            update(existing.copy(
                wordCount = wordCount,
                wordsWritten = existing.wordsWritten + wordsWritten,
                chaptersWritten = existing.chaptersWritten + chaptersWritten,
                editDuration = existing.editDuration + editDuration
            ))
        } else {
            insert(WritingStatsEntity(
                novelId = novelId,
                date = date,
                wordCount = wordCount,
                wordsWritten = wordsWritten,
                chaptersWritten = chaptersWritten,
                editDuration = editDuration
            ))
        }
    }
}
