package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.ChatSessionEntity
import com.bicy.novel.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createSession(session: ChatSessionEntity): Long
    
    @Update
    suspend fun updateSession(session: ChatSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)
    
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
    
    @Query("SELECT * FROM chat_sessions WHERE novelId = :novelId ORDER BY updatedAt DESC")
    fun getSessionsByNovelId(novelId: Long): Flow<List<ChatSessionEntity>>
    
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChatSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createMessage(message: ChatMessageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createMessages(messages: List<ChatMessageEntity>)
    
    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)
    
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: Long)
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesBySessionOnce(sessionId: Long): List<ChatMessageEntity>
    
    @Query("SELECT COUNT(*) FROM chat_sessions WHERE novelId = :novelId")
    suspend fun getSessionCount(novelId: Long): Int
}
