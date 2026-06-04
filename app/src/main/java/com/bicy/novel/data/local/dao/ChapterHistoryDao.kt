package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.ContentHistoryEntity
import com.bicy.novel.data.local.entity.ContentHistoryTarget
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentHistoryDao {
    
    @Query("SELECT * FROM content_history WHERE targetId = :targetId AND targetType = :targetType ORDER BY savedAt DESC")
    fun getHistoryByTarget(targetId: Long, targetType: String): Flow<List<ContentHistoryEntity>>
    
    @Query("SELECT * FROM content_history WHERE targetId = :targetId AND targetType = :targetType ORDER BY savedAt DESC LIMIT :limit")
    suspend fun getRecentHistory(targetId: Long, targetType: String, limit: Int = 20): List<ContentHistoryEntity>
    
    @Query("SELECT * FROM content_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): ContentHistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ContentHistoryEntity): Long
    
    @Update
    suspend fun update(history: ContentHistoryEntity)
    
    @Delete
    suspend fun delete(history: ContentHistoryEntity)
    
    @Query("DELETE FROM content_history WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM content_history WHERE targetId = :targetId AND targetType = :targetType")
    suspend fun deleteAllByTarget(targetId: Long, targetType: String)
    
    @Query("DELETE FROM content_history WHERE targetId = :targetId AND targetType = :targetType AND savedAt < :beforeTime")
    suspend fun deleteOldHistory(targetId: Long, targetType: String, beforeTime: Long)
    
    @Query("SELECT COUNT(*) FROM content_history WHERE targetId = :targetId AND targetType = :targetType")
    suspend fun getHistoryCount(targetId: Long, targetType: String): Int
    
    @Query("DELETE FROM content_history WHERE id IN (SELECT id FROM content_history WHERE targetId = :targetId AND targetType = :targetType ORDER BY savedAt DESC LIMIT -1 OFFSET :keepCount)")
    suspend fun trimExcessHistory(targetId: Long, targetType: String, keepCount: Int)
    
    @Query("SELECT DISTINCT targetType, targetId FROM content_history")
    suspend fun getAllTargetPairs(): List<ContentHistoryTarget>
    
    @Query("SELECT COUNT(*) FROM content_history")
    suspend fun getTotalHistoryCount(): Int
    
    @Transaction
    suspend fun saveHistory(
        targetType: String,
        targetId: Long,
        title: String,
        content: String,
        wordCount: Int,
        note: String? = null
    ) {
        insert(ContentHistoryEntity(
            targetType = targetType,
            targetId = targetId,
            title = title,
            content = content,
            wordCount = wordCount,
            note = note
        ))
        // 保留最近50条，删除多余
        trimExcessHistory(targetId, targetType, 50)
    }
}
