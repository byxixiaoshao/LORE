package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Novel
import com.bicy.novel.domain.model.WritingStats
import com.bicy.novel.domain.repository.ChapterRepository
import com.bicy.novel.domain.repository.CharacterRepository
import com.bicy.novel.domain.repository.NovelRepository
import com.bicy.novel.domain.repository.NoteRepository
import com.bicy.novel.domain.repository.TimelineRepository
import com.bicy.novel.domain.repository.WritingStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NovelStats(
    val wordCount: Int = 0,
    val chapterCount: Int = 0,
    val characterCount: Int = 0,
    val noteCount: Int = 0,
    val timelineEventCount: Int = 0,
    val totalWritingDuration: Long = 0L,
    val totalEditCount: Int = 0
)

data class EditTarget(
    val id: Long,
    val type: String
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val novelRepository: NovelRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository,
    private val writingStatsRepository: WritingStatsRepository
) : ViewModel() {
    
    private val _novel = MutableStateFlow<Novel?>(null)
    val novel = _novel.asStateFlow()
    
    private val _stats = MutableStateFlow(NovelStats())
    val stats = _stats.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    
    private val _editTarget = MutableStateFlow<EditTarget?>(null)
    val editTarget = _editTarget.asStateFlow()
    
    private val _showNewChapterDialog = MutableStateFlow(false)
    val showNewChapterDialog = _showNewChapterDialog.asStateFlow()
    
    // 最近7天的统计数据
    private val _recentStats = MutableStateFlow<List<WritingStats>>(emptyList())
    val recentStats = _recentStats.asStateFlow()
    
    fun loadNovel(novelId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val novel = novelRepository.getNovelById(novelId)
            _novel.value = novel
            
            if (novel != null) {
                loadStats(novelId)
                loadRecentStats(novelId)
            }
            
            _isLoading.value = false
        }
    }
    
    private suspend fun loadStats(novelId: Long) {
        try {
            val wordCount = chapterRepository.getTotalWordCount(novelId)
            val chapterCount = chapterRepository.getChapterCount(novelId)
            val timelineEventCount = timelineRepository.getKeyEventCount(novelId)
            val totalEditDuration = writingStatsRepository.getTotalEditDuration(novelId)
            val totalWordsWritten = writingStatsRepository.getTotalWordsWritten(novelId)
            
            _stats.value = NovelStats(
                wordCount = wordCount,
                chapterCount = chapterCount,
                characterCount = 0,
                noteCount = 0,
                timelineEventCount = timelineEventCount,
                totalWritingDuration = totalEditDuration,
                totalEditCount = totalWordsWritten
            )
        } catch (e: Exception) {
            _stats.value = NovelStats()
        }
    }
    
    private suspend fun loadRecentStats(novelId: Long) {
        try {
            val calendar = java.util.Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
            val startTime = calendar.timeInMillis
            
            val stats = writingStatsRepository.getStatsInRange(novelId, startTime, endTime)
            _recentStats.value = stats
        } catch (e: Exception) {
            _recentStats.value = emptyList()
        }
    }
    
    fun determineEditTarget(onResult: (EditTarget?) -> Unit) {
        viewModelScope.launch {
            val novel = _novel.value
            if (novel == null) {
                onResult(null)
                return@launch
            }
            val novelId = novel.id
            
            if (novel.lastEditedId != null && novel.lastEditedId > 0) {
                onResult(EditTarget(novel.lastEditedId, novel.lastEditedType))
                return@launch
            }
            
            val chapters = chapterRepository.getChaptersByNovelIdOnce(novelId)
            if (chapters.isNotEmpty()) {
                val firstChapter = chapters.sortedBy { it.sortOrder }.first()
                onResult(EditTarget(firstChapter.id, Novel.TYPE_CHAPTER))
            } else {
                onResult(null)
            }
        }
    }
    
    fun checkShouldShowNewChapterDialog(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val novel = _novel.value
            if (novel == null) {
                onResult(false)
                return@launch
            }
            val chapters = chapterRepository.getChaptersByNovelIdOnce(novel.id)
            onResult(chapters.isEmpty())
        }
    }
    
    fun updateNovelLock(novelId: Long, isLocked: Boolean) {
        viewModelScope.launch {
            novelRepository.updateNovelLock(novelId, isLocked)
            val novel = novelRepository.getNovelById(novelId)
            _novel.value = novel
        }
    }
}
