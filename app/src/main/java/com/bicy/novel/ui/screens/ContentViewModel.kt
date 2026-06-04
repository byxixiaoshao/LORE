package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Chapter
import com.bicy.novel.domain.repository.ChapterRepository
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
class ContentViewModel @Inject constructor(
    private val chapterRepository: ChapterRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    val novelId = _novelId.asStateFlow()
    
    val chapters = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) {
            chapterRepository.getChaptersByNovelId(novelId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _navigateToEdit = MutableStateFlow<Pair<Long, Long?>?>(null)
    val navigateToEdit = _navigateToEdit.asStateFlow()
    
    fun setNovelId(id: Long) {
        _novelId.value = id
    }
    
    fun createChapter() {
        viewModelScope.launch {
            val id = _novelId.value
            if (id > 0) {
                val chapter = Chapter(
                    novelId = id,
                    title = "新章节"
                )
                val newChapterId = chapterRepository.createChapter(chapter)
                _navigateToEdit.value = Pair(id, newChapterId)
            }
        }
    }
    
    fun clearNavigation() {
        _navigateToEdit.value = null
    }
    
    fun deleteChapters(chapterIds: List<Long>) {
        viewModelScope.launch {
            chapterIds.forEach { id ->
                chapterRepository.deleteChapter(id)
            }
        }
    }
}
