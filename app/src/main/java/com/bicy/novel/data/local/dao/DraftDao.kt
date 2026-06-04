package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(draft: DraftEntity): Long
    
    @Query("SELECT * FROM drafts WHERE targetType = :targetType AND targetId = :targetId LIMIT 1")
    suspend fun getDraft(targetType: String, targetId: Long): DraftEntity?
    
    @Query("SELECT * FROM drafts WHERE novelId = :novelId")
    suspend fun getDraftsByNovelId(novelId: Long): List<DraftEntity>
    
    @Query("SELECT * FROM drafts WHERE novelId = :novelId")
    fun getDraftsByNovelIdFlow(novelId: Long): Flow<List<DraftEntity>>
    
    @Query("SELECT COUNT(*) FROM drafts WHERE novelId = :novelId")
    suspend fun getDraftCount(novelId: Long): Int
    
    @Delete
    suspend fun delete(draft: DraftEntity)
    
    @Query("DELETE FROM drafts WHERE targetType = :targetType AND targetId = :targetId")
    suspend fun deleteDraft(targetType: String, targetId: Long)
    
    @Query("DELETE FROM drafts WHERE novelId = :novelId")
    suspend fun deleteDraftsByNovelId(novelId: Long)
}
