package com.bicy.novel.domain.repository

import androidx.sqlite.db.SupportSQLiteDatabase
import com.bicy.novel.data.local.dao.ContentHistoryDao
import com.bicy.novel.data.local.database.AppDatabase
import com.bicy.novel.data.local.entity.ContentHistoryEntity
import com.bicy.novel.domain.model.ContentHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentHistoryRepository @Inject constructor(
    private val contentHistoryDao: ContentHistoryDao,
    private val db: AppDatabase
) {
    fun getHistoryByTarget(targetId: Long, targetType: String): Flow<List<ContentHistory>> {
        return contentHistoryDao.getHistoryByTarget(targetId, targetType).map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    suspend fun getRecentHistory(targetId: Long, targetType: String, limit: Int = 20): List<ContentHistory> {
        return contentHistoryDao.getRecentHistory(targetId, targetType, limit).map { it.toModel() }
    }
    
    suspend fun getHistoryById(id: Long): ContentHistory? {
        return contentHistoryDao.getHistoryById(id)?.toModel()
    }
    
    suspend fun saveHistory(
        targetType: String,
        targetId: Long,
        title: String,
        content: String,
        wordCount: Int,
        note: String? = null
    ) {
        contentHistoryDao.saveHistory(targetType, targetId, title, content, wordCount, note)
    }
    
    suspend fun deleteHistory(id: Long) {
        contentHistoryDao.deleteById(id)
    }
    
    suspend fun deleteAllHistory(targetId: Long, targetType: String) {
        contentHistoryDao.deleteAllByTarget(targetId, targetType)
    }
    
    suspend fun getHistoryCount(targetId: Long, targetType: String): Int {
        return contentHistoryDao.getHistoryCount(targetId, targetType)
    }
    
    /**
     * 一次性清理所有超出的历史记录（每个target保留最近50条）
     * @return 删除的记录数
     */
    suspend fun cleanUpExcessHistory(): Int {
        val before = contentHistoryDao.getTotalHistoryCount()
        val pairs = contentHistoryDao.getAllTargetPairs()
        for (pair in pairs) {
            contentHistoryDao.trimExcessHistory(pair.targetId, pair.targetType, 50)
        }
        val after = contentHistoryDao.getTotalHistoryCount()
        return before - after
    }
    
    /**
     * 执行VACUUM压缩数据库文件（先checkpoint WAL再VACUUM）
     */
    suspend fun vacuum() {
        withContext(Dispatchers.IO) {
            val d = db.openHelper.writableDatabase
            d.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            d.execSQL("VACUUM")
        }
    }
    
    /**
     * 诊断各表数据量，返回每张表的行数 + 文件大小
     */
    suspend fun diagnoseTableSize(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        val d = this@ContentHistoryRepository.db.openHelper.writableDatabase
        val tables = listOf(
            "chapters", "volumes", "novels", "characters", "worldview",
            "notes", "timeline", "content_history", "chat_messages",
            "chat_sessions", "ai_operations", "drafts", "writing_stats"
        )
        for (table in tables) {
            val cursor = d.query("SELECT COUNT(*) FROM $table")
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            sb.appendLine("$table: $count rows")
        }
        
        // 文件大小
        if (db.openHelper.writableDatabase.path != null) {
            val dbFile = java.io.File(db.openHelper.writableDatabase.path!!)
            if (dbFile.exists()) {
                sb.appendLine()
                sb.appendLine("DB file: ${dbFile.length() / 1024}KB")
                val wal = java.io.File(dbFile.path + "-wal")
                if (wal.exists()) sb.appendLine("WAL file: ${wal.length() / 1024}KB")
                val shm = java.io.File(dbFile.path + "-shm")
                if (shm.exists()) sb.appendLine("SHM file: ${shm.length() / 1024}KB")
            }
        }
        sb.toString()
    }
    
    private fun ContentHistoryEntity.toModel() = ContentHistory(
        id = id,
        targetType = targetType,
        targetId = targetId,
        title = title,
        content = content,
        wordCount = wordCount,
        savedAt = savedAt,
        note = note
    )
}
