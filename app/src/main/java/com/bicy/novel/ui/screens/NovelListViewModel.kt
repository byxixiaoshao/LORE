package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Novel
import com.bicy.novel.domain.repository.NovelRepository
import com.bicy.novel.util.SearchUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class NovelListViewModel @Inject constructor(
    private val novelRepository: NovelRepository
) : ViewModel() {
    
    val novels: StateFlow<List<Novel>> = novelRepository.getAllNovels()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()
    
    // 使用优化的防抖搜索
    val searchResults: StateFlow<List<Novel>> = _searchQuery
        .debounce(300) // 300ms防抖
        .distinctUntilChanged()
        .combine(novels) { query, allNovels ->
            if (query.isBlank()) {
                _isSearching.value = false
                emptyList()
            } else {
                _isSearching.value = true
                // 使用模糊匹配和排序
                SearchUtils.searchAndSort(allNovels, query) { novel ->
                    "${novel.title} ${novel.author} ${novel.description}"
                }
            }
        }
        .catch { e ->
            emit(emptyList())
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun deleteNovel(novelId: Long) {
        viewModelScope.launch {
            novelRepository.deleteNovel(novelId)
        }
    }
}
