package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND isDeleted = 0 ORDER BY sortOrder ASC")
    fun getChaptersByNovelId(novelId: Long): Flow<List<ChapterEntity>>
    
    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND isDeleted = 0 ORDER BY sortOrder ASC")
    suspend fun getChaptersByNovelIdOnce(novelId: Long): List<ChapterEntity>
    
    @Query("SELECT * FROM chapters WHERE volumeId = :volumeId AND isDeleted = 0 ORDER BY sortOrder ASC")
    fun getChaptersByVolumeId(volumeId: Long): Flow<List<ChapterEntity>>
    
    @Query("SELECT * FROM chapters WHERE id = :id AND isDeleted = 0")
    suspend fun getChapterById(id: Long): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE id = :id AND isDeleted = 0")
    fun getChapterByIdFlow(id: Long): Flow<ChapterEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long
    
    @Update
    suspend fun updateChapter(chapter: ChapterEntity)
    
    @Query("UPDATE chapters SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteChapter(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)
    
    @Query("DELETE FROM chapters WHERE novelId = :novelId")
    suspend fun deleteChaptersByNovelId(novelId: Long)
    
    @Query("SELECT MAX(sortOrder) FROM chapters WHERE novelId = :novelId")
    suspend fun getMaxSortOrder(novelId: Long): Int?
    
    @Query("SELECT SUM(wordCount) FROM chapters WHERE novelId = :novelId AND isDeleted = 0")
    suspend fun getTotalWordCount(novelId: Long): Int?
    
    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId AND isDeleted = 0")
    suspend fun getChapterCount(novelId: Long): Int
    
    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND isDeleted = 0 ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getFirstChapter(novelId: Long): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND sortOrder > :currentSortOrder AND isDeleted = 0 ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getNextChapter(novelId: Long, currentSortOrder: Int): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND sortOrder < :currentSortOrder AND isDeleted = 0 ORDER BY sortOrder DESC LIMIT 1")
    suspend fun getPreviousChapter(novelId: Long, currentSortOrder: Int): ChapterEntity?
}
