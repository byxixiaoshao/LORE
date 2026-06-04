package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Chapter
import com.bicy.novel.domain.repository.ChapterRepository
import com.bicy.novel.domain.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChapterListViewModel @Inject constructor(
    private val chapterRepository: ChapterRepository,
    private val novelRepository: NovelRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    val novelId = _novelId.asStateFlow()
    
    private val _novelTitle = MutableStateFlow("")
    val novelTitle = _novelTitle.asStateFlow()
    
    val chapters = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) {
            chapterRepository.getChaptersByNovelId(novelId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun setNovelId(id: Long) {
        _novelId.value = id
        viewModelScope.launch {
            val novel = novelRepository.getNovelById(id)
            _novelTitle.value = novel?.title ?: ""
        }
    }
    
    fun deleteChapter(chapterId: Long) {
        viewModelScope.launch {
            chapterRepository.deleteChapter(chapterId)
            updateNovelStats()
        }
    }
    
    private suspend fun updateNovelStats() {
        val id = _novelId.value
        val wordCount = chapterRepository.getTotalWordCount(id)
        val chapterCount = chapterRepository.getChapterCount(id)
        novelRepository.updateNovelStats(id, wordCount, chapterCount)
    }
}
