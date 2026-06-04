package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Note
import com.bicy.novel.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    
    private val _category = MutableStateFlow("")
    val category = _category.asStateFlow()
    
    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()
    
    private var noteId: Long? = null
    private var novelId: Long? = null
    
    fun init(novelIdFromNav: Long?, noteIdFromNav: Long?) {
        novelId = novelIdFromNav
        noteId = noteIdFromNav
        
        noteIdFromNav?.let { id ->
            if (id > 0) {
                loadNote(id)
            }
        }
    }
    
    private fun loadNote(id: Long) {
        viewModelScope.launch {
            val note = noteRepository.getNoteById(id)
            note?.let {
                _title.value = it.title
                _category.value = it.category
                _content.value = it.content
                novelId = it.novelId
            }
        }
    }
    
    fun setTitle(value: String) { _title.value = value }
    fun setCategory(value: String) { _category.value = value }
    fun setContent(value: String) { _content.value = value }
    
    fun saveNote(onSaved: () -> Unit) {
        if (_title.value.isBlank()) return
        
        viewModelScope.launch {
            val note = Note(
                id = noteId ?: 0,
                novelId = novelId,
                title = _title.value,
                category = _category.value,
                content = _content.value
            )
            
            if (noteId == null || noteId == 0L) {
                noteRepository.createNote(note)
            } else {
                noteRepository.updateNote(note)
            }
            onSaved()
        }
    }
}
