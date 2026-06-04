package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.VolumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VolumeDao {
    @Query("SELECT * FROM volumes WHERE novelId = :novelId ORDER BY sortOrder ASC")
    fun getVolumesByNovelId(novelId: Long): Flow<List<VolumeEntity>>
    
    @Query("SELECT * FROM volumes WHERE id = :id")
    suspend fun getVolumeById(id: Long): VolumeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVolume(volume: VolumeEntity): Long
    
    @Update
    suspend fun updateVolume(volume: VolumeEntity)
    
    @Delete
    suspend fun deleteVolume(volume: VolumeEntity)
    
    @Query("DELETE FROM volumes WHERE novelId = :novelId")
    suspend fun deleteVolumesByNovelId(novelId: Long)
    
    @Query("SELECT MAX(sortOrder) FROM volumes WHERE novelId = :novelId")
    suspend fun getMaxSortOrder(novelId: Long): Int?
}
