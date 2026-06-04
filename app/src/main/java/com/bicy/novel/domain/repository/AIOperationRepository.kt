package com.bicy.novel.domain.repository

import com.bicy.novel.data.local.dao.AIOperationDao
import com.bicy.novel.data.local.entity.AIOperationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIOperationRepository @Inject constructor(
    private val aiOperationDao: AIOperationDao
) {
    suspend fun insert(operation: AIOperationEntity): Long {
        return aiOperationDao.insert(operation)
    }
    
    suspend fun insertAll(operations: List<AIOperationEntity>) {
        aiOperationDao.insertAll(operations)
    }
    
    suspend fun getByMessageId(messageId: Long): List<AIOperationEntity> {
        return aiOperationDao.getByMessageId(messageId)
    }
    
    suspend fun getBySessionId(sessionId: Long): List<AIOperationEntity> {
        return aiOperationDao.getBySessionId(sessionId)
    }
    
    fun getByMessageIdFlow(messageId: Long): Flow<List<AIOperationEntity>> {
        return aiOperationDao.getByMessageIdFlow(messageId)
    }
    
    fun getBySessionIdFlow(sessionId: Long): Flow<List<AIOperationEntity>> {
        return aiOperationDao.getBySessionIdFlow(sessionId)
    }
    
    suspend fun deleteByMessageId(messageId: Long) {
        aiOperationDao.deleteByMessageId(messageId)
    }
    
    suspend fun deleteBySessionId(sessionId: Long) {
        aiOperationDao.deleteBySessionId(sessionId)
    }
    
    suspend fun deleteAll() {
        aiOperationDao.deleteAll()
    }
}
