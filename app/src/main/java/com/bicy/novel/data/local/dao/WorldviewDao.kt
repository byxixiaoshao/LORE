package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.WorldviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldviewDao {
    @Query("SELECT * FROM worldview WHERE novelId = :novelId ORDER BY createdAt DESC")
    fun getWorldviewsByNovelId(novelId: Long): Flow<List<WorldviewEntity>>
    
    @Query("SELECT * FROM worldview WHERE novelId = :novelId ORDER BY createdAt DESC")
    suspend fun getWorldviewsByNovelIdOnce(novelId: Long): List<WorldviewEntity>
    
    @Query("SELECT * FROM worldview WHERE id = :id")
    suspend fun getWorldviewById(id: Long): WorldviewEntity?
    
    @Query("SELECT * FROM worldview WHERE novelId = :novelId AND category = :category")
    fun getWorldviewsByCategory(novelId: Long, category: String): Flow<List<WorldviewEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorldview(worldview: WorldviewEntity): Long
    
    @Update
    suspend fun updateWorldview(worldview: WorldviewEntity)
    
    @Delete
    suspend fun deleteWorldview(worldview: WorldviewEntity)
    
    @Query("DELETE FROM worldview WHERE novelId = :novelId")
    suspend fun deleteWorldviewsByNovelId(novelId: Long)
}
