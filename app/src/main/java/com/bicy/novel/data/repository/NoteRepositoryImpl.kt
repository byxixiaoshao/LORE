package com.bicy.novel.data.repository

import com.bicy.novel.data.local.dao.NoteDao
import com.bicy.novel.data.local.entity.NoteEntity
import com.bicy.novel.domain.model.Note
import com.bicy.novel.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {
    
    override fun getNotesByNovelId(novelId: Long): Flow<List<Note>> {
        return noteDao.getNotesByNovelId(novelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getNotesByNovelIdOnce(novelId: Long): List<Note> {
        return noteDao.getNotesByNovelIdOnce(novelId).map { it.toDomain() }
    }
    
    override fun getGlobalNotes(): Flow<List<Note>> {
        return noteDao.getGlobalNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)?.toDomain()
    }
    
    override suspend fun createNote(note: Note): Long {
        return noteDao.insertNote(note.toEntity())
    }
    
    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.toEntity())
    }
    
    override suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note.toEntity())
    }
    
    private fun NoteEntity.toDomain() = Note(
        id = id,
        novelId = novelId,
        title = title,
        content = content,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun Note.toEntity() = NoteEntity(
        id = id,
        novelId = novelId,
        title = title,
        content = content,
        category = category,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
