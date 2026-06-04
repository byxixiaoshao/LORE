package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE novelId = :novelId ORDER BY createdAt DESC")
    fun getCharactersByNovelId(novelId: Long): Flow<List<CharacterEntity>>
    
    @Query("SELECT * FROM characters WHERE novelId = :novelId ORDER BY createdAt DESC")
    suspend fun getCharactersByNovelIdOnce(novelId: Long): List<CharacterEntity>
    
    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: Long): CharacterEntity?
    
    @Query("SELECT * FROM characters WHERE novelId = :novelId AND name LIKE '%' || :keyword || '%'")
    fun searchCharacters(novelId: Long, keyword: String): Flow<List<CharacterEntity>>
    
    @Query("SELECT * FROM characters WHERE novelId = :novelId AND roleType = :roleType")
    fun getCharactersByRoleType(novelId: Long, roleType: String): Flow<List<CharacterEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long
    
    @Update
    suspend fun updateCharacter(character: CharacterEntity)
    
    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)
    
    @Query("DELETE FROM characters WHERE novelId = :novelId")
    suspend fun deleteCharactersByNovelId(novelId: Long)
}
