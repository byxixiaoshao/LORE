package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.AIOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIOperationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: AIOperationEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<AIOperationEntity>)
    
    @Update
    suspend fun update(operation: AIOperationEntity)
    
    @Delete
    suspend fun delete(operation: AIOperationEntity)
    
    @Query("DELETE FROM ai_operations WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM ai_operations WHERE messageId = :messageId")
    suspend fun deleteByMessageId(messageId: Long)
    
    @Query("DELETE FROM ai_operations WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
    
    @Query("SELECT * FROM ai_operations WHERE id = :id")
    suspend fun getById(id: Long): AIOperationEntity?
    
    @Query("SELECT * FROM ai_operations WHERE messageId = :messageId ORDER BY timestamp DESC")
    suspend fun getByMessageId(messageId: Long): List<AIOperationEntity>
    
    @Query("SELECT * FROM ai_operations WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getBySessionId(sessionId: Long): List<AIOperationEntity>
    
    @Query("SELECT * FROM ai_operations WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBySessionIdFlow(sessionId: Long): Flow<List<AIOperationEntity>>
    
    @Query("SELECT * FROM ai_operations WHERE messageId = :messageId ORDER BY timestamp DESC")
    fun getByMessageIdFlow(messageId: Long): Flow<List<AIOperationEntity>>
    
    @Query("SELECT * FROM ai_operations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<AIOperationEntity>
    
    @Query("DELETE FROM ai_operations")
    suspend fun deleteAll()
}
