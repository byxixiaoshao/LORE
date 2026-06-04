package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.NovelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    @Query("SELECT * FROM novels WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllNovels(): Flow<List<NovelEntity>>
    
    @Query("SELECT * FROM novels WHERE id = :id AND isDeleted = 0")
    suspend fun getNovelById(id: Long): NovelEntity?
    
    @Query("SELECT * FROM novels WHERE id = :id AND isDeleted = 0")
    fun getNovelByIdFlow(id: Long): Flow<NovelEntity?>
    
    @Query("SELECT * FROM novels WHERE title LIKE '%' || :keyword || '%' AND isDeleted = 0")
    fun searchNovels(keyword: String): Flow<List<NovelEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: NovelEntity): Long
    
    @Update
    suspend fun updateNovel(novel: NovelEntity)
    
    @Query("UPDATE novels SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteNovel(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM novels WHERE id = :id")
    suspend fun deleteNovelPermanently(id: Long)
    
    @Query("UPDATE novels SET totalWords = :wordCount, chapterCount = :chapterCount, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateNovelStats(id: Long, wordCount: Int, chapterCount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE novels SET isLocked = :isLocked, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateNovelLock(id: Long, isLocked: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT SUM(totalWords) FROM novels WHERE isDeleted = 0")
    suspend fun getTotalWords(): Int?
}
