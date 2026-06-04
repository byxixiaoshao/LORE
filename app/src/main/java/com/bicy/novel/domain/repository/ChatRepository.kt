package com.bicy.novel.domain.repository

import com.bicy.novel.data.local.dao.ChatDao
import com.bicy.novel.data.local.entity.ChatSessionEntity
import com.bicy.novel.data.local.entity.ChatMessageEntity
import com.bicy.novel.domain.model.ChatSession
import com.bicy.novel.domain.model.ChatMsg
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao
) {
    fun getSessionsByNovelId(novelId: Long): Flow<List<ChatSession>> {
        return chatDao.getSessionsByNovelId(novelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getSessionById(sessionId: Long): ChatSession? {
        return chatDao.getSessionById(sessionId)?.toDomain()
    }
    
    suspend fun createSession(session: ChatSession): Long {
        return chatDao.createSession(session.toEntity())
    }
    
    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session.toEntity())
    }
    
    suspend fun deleteSession(sessionId: Long) {
        chatDao.deleteSessionById(sessionId)
    }
    
    fun getMessagesBySession(sessionId: Long): Flow<List<ChatMsg>> {
        return chatDao.getMessagesBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getMessagesBySessionOnce(sessionId: Long): List<ChatMsg> {
        return chatDao.getMessagesBySessionOnce(sessionId).map { it.toDomain() }
    }
    
    suspend fun addMessage(message: ChatMsg): Long {
        return chatDao.createMessage(message.toEntity())
    }
    
    suspend fun addMessages(messages: List<ChatMsg>) {
        chatDao.createMessages(messages.map { it.toEntity() })
    }
    
    suspend fun deleteMessagesBySession(sessionId: Long) {
        chatDao.deleteMessagesBySession(sessionId)
    }
    
    private fun ChatSessionEntity.toDomain() = ChatSession(
        id = id,
        novelId = novelId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun ChatSession.toEntity() = ChatSessionEntity(
        id = id,
        novelId = novelId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun ChatMessageEntity.toDomain() = ChatMsg(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        timestamp = timestamp
    )
    
    private fun ChatMsg.toEntity() = ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        timestamp = timestamp
    )
}
