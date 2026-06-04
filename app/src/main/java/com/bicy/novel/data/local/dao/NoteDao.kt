package com.bicy.novel.data.local.dao

import androidx.room.*
import com.bicy.novel.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE novelId = :novelId ORDER BY createdAt DESC")
    fun getNotesByNovelId(novelId: Long): Flow<List<NoteEntity>>
    
    @Query("SELECT * FROM notes WHERE novelId = :novelId ORDER BY createdAt DESC")
    suspend fun getNotesByNovelIdOnce(novelId: Long): List<NoteEntity>
    
    @Query("SELECT * FROM notes WHERE novelId IS NULL ORDER BY createdAt DESC")
    fun getGlobalNotes(): Flow<List<NoteEntity>>
    
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long
    
    @Update
    suspend fun updateNote(note: NoteEntity)
    
    @Delete
    suspend fun deleteNote(note: NoteEntity)
    
    @Query("DELETE FROM notes WHERE novelId = :novelId")
    suspend fun deleteNotesByNovelId(novelId: Long)
}
