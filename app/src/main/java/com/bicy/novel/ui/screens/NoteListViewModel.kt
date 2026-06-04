package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Note
import com.bicy.novel.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    
    val notes = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) {
            noteRepository.getNotesByNovelId(novelId)
        } else {
            noteRepository.getGlobalNotes()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun setNovelId(id: Long) {
        _novelId.value = id
    }
    
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }
}
