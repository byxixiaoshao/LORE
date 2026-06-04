package com.bicy.novel.domain.repository

import com.bicy.novel.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotesByNovelId(novelId: Long): Flow<List<Note>>
    suspend fun getNotesByNovelIdOnce(novelId: Long): List<Note>
    fun getGlobalNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    suspend fun createNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
}
