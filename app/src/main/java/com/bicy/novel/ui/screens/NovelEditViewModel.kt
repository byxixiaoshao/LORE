package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Novel
import com.bicy.novel.domain.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NovelEditViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {
    
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    
    private val _author = MutableStateFlow("")
    val author = _author.asStateFlow()
    
    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    
    private val _category = MutableStateFlow("")
    val category = _category.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private var novelId: Long? = null
    
    fun loadNovel(id: Long) {
        novelId = id
        viewModelScope.launch {
            _isLoading.value = true
            val novel = novelRepository.getNovelById(id)
            novel?.let {
                _title.value = it.title
                _author.value = it.author
                _description.value = it.description
                _category.value = it.category
            }
            _isLoading.value = false
        }
    }
    
    fun setTitle(value: String) { _title.value = value }
    fun setAuthor(value: String) { _author.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setCategory(value: String) { _category.value = value }
    
    fun saveNovel(onSaved: () -> Unit) {
        if (_title.value.isBlank()) return
        
        viewModelScope.launch {
            val novel = Novel(
                id = novelId ?: 0,
                title = _title.value,
                author = _author.value,
                description = _description.value,
                category = _category.value
            )
            
            if (novelId == null) {
                novelRepository.createNovel(novel)
            } else {
                novelRepository.updateNovel(novel)
            }
            onSaved()
        }
    }
}
