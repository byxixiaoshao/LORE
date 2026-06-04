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
class NovelDetailViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {
    
    private val _novel = MutableStateFlow<Novel?>(null)
    val novel = _novel.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    
    fun loadNovel(novelId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            novelRepository.getNovelByIdFlow(novelId).collect { novel ->
                _novel.value = novel
                _isLoading.value = false
            }
        }
    }
    
    fun deleteNovel(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _novel.value?.let { novel ->
                novelRepository.deleteNovel(novel.id)
                onDeleted()
            }
        }
    }
}
