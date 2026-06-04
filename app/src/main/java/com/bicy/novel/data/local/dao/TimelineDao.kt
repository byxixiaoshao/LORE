package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.TimelineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline WHERE novelId = :novelId ORDER BY sortOrder ASC")
    fun getTimelineByNovelId(novelId: Long): Flow<List<TimelineEntity>>
    
    @Query("SELECT * FROM timeline WHERE novelId = :novelId ORDER BY sortOrder ASC")
    suspend fun getTimelineByNovelIdOnce(novelId: Long): List<TimelineEntity>
    
    @Query("SELECT * FROM timeline WHERE id = :id")
    suspend fun getTimelineById(id: Long): TimelineEntity?
    
    @Query("SELECT * FROM timeline WHERE novelId = :novelId AND isKeyEvent = 1 ORDER BY sortOrder ASC")
    fun getKeyEvents(novelId: Long): Flow<List<TimelineEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeline(timeline: TimelineEntity): Long
    
    @Update
    suspend fun updateTimeline(timeline: TimelineEntity)
    
    @Delete
    suspend fun deleteTimeline(timeline: TimelineEntity)
    
    @Query("DELETE FROM timeline WHERE novelId = :novelId")
    suspend fun deleteTimelineByNovelId(novelId: Long)
    
    @Query("SELECT MAX(sortOrder) FROM timeline WHERE novelId = :novelId")
    suspend fun getMaxSortOrder(novelId: Long): Int?
    
    @Query("SELECT COUNT(*) FROM timeline WHERE novelId = :novelId AND isKeyEvent = 1")
    suspend fun getKeyEventCount(novelId: Long): Int
}
