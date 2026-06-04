package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Note
import com.bicy.novel.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    
    val notes = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) {
            noteRepository.getNotesByNovelId(novelId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun setNovelId(id: Long) {
        _novelId.value = id
    }
    
    fun createNote() {
        viewModelScope.launch {
            val id = _novelId.value
            if (id > 0) {
                noteRepository.createNote(
                    Note(
                        novelId = id,
                        title = "新笔记"
                    )
                )
            }
        }
    }
    
    /**
     * 批量删除笔记
     * @param noteIds 要删除的笔记ID列表
     */
    fun deleteNotes(noteIds: List<Long>) {
        viewModelScope.launch {
            noteIds.forEach { id ->
                val note = noteRepository.getNoteById(id)
                note?.let { noteRepository.deleteNote(it) }
            }
        }
    }
}
