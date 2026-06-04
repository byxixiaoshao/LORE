package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.ContentHistory
import com.bicy.novel.domain.repository.ContentHistoryRepository
import com.bicy.novel.domain.repository.ChapterRepository
import com.bicy.novel.domain.repository.CharacterRepository
import com.bicy.novel.domain.repository.WorldviewRepository
import com.bicy.novel.domain.repository.NoteRepository
import com.bicy.novel.domain.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContentHistoryState(
    val histories: List<ContentHistory> = emptyList(),
    val selectedHistory: ContentHistory? = null,
    val isLoading: Boolean = false,
    val showRestoreDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val currentContent: String = ""
)

@HiltViewModel
class ContentHistoryViewModel @Inject constructor(
    private val contentHistoryRepository: ContentHistoryRepository,
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ContentHistoryState())
    val state = _state.asStateFlow()
    
    fun loadHistory(targetId: Long, targetType: String = "chapter") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            // 获取当前内容
            val currentContent = when (targetType) {
                "chapter" -> chapterRepository.getChapterById(targetId)?.content ?: ""
                "character" -> characterRepository.getCharacterById(targetId)?.description ?: ""
                "worldview" -> worldviewRepository.getWorldviewById(targetId)?.content ?: ""
                "note" -> noteRepository.getNoteById(targetId)?.content ?: ""
                "timeline" -> timelineRepository.getTimelineById(targetId)?.description ?: ""
                else -> ""
            }
            
            contentHistoryRepository.getHistoryByTarget(targetId, targetType).collect { histories ->
                _state.value = _state.value.copy(
                    histories = histories,
                    isLoading = false,
                    currentContent = currentContent
                )
            }
        }
    }
    
    fun selectHistory(history: ContentHistory) {
        _state.value = _state.value.copy(selectedHistory = history)
    }
    
    fun showRestoreDialog() {
        _state.value = _state.value.copy(showRestoreDialog = true)
    }
    
    fun hideRestoreDialog() {
        _state.value = _state.value.copy(showRestoreDialog = false)
    }
    
    fun showDeleteDialog() {
        _state.value = _state.value.copy(showDeleteDialog = true)
    }
    
    fun hideDeleteDialog() {
        _state.value = _state.value.copy(showDeleteDialog = false)
    }
    
    fun restoreHistory(onRestored: () -> Unit) {
        val history = _state.value.selectedHistory ?: return
        
        viewModelScope.launch {
            when (history.targetType) {
                "chapter" -> {
                    val chapter = chapterRepository.getChapterById(history.targetId)
                    if (chapter != null) {
                        chapterRepository.updateChapter(
                            chapter.copy(
                                title = history.title,
                                content = history.content,
                                wordCount = history.wordCount
                            )
                        )
                    }
                }
                "character" -> {
                    val character = characterRepository.getCharacterById(history.targetId)
                    if (character != null) {
                        characterRepository.updateCharacter(
                            character.copy(
                                name = history.title,
                                description = history.content
                            )
                        )
                    }
                }
                "worldview" -> {
                    val worldview = worldviewRepository.getWorldviewById(history.targetId)
                    if (worldview != null) {
                        worldviewRepository.updateWorldview(
                            worldview.copy(
                                title = history.title,
                                content = history.content
                            )
                        )
                    }
                }
                "note" -> {
                    val note = noteRepository.getNoteById(history.targetId)
                    if (note != null) {
                        noteRepository.updateNote(
                            note.copy(
                                title = history.title,
                                content = history.content
                            )
                        )
                    }
                }
                "timeline" -> {
                    val event = timelineRepository.getTimelineById(history.targetId)
                    if (event != null) {
                        timelineRepository.updateTimeline(
                            event.copy(
                                title = history.title,
                                description = history.content
                            )
                        )
                    }
                }
            }
            
            // 保存新的历史记录
            contentHistoryRepository.saveHistory(
                targetType = history.targetType,
                targetId = history.targetId,
                title = history.title,
                content = history.content,
                wordCount = history.wordCount,
                note = "恢复自 ${formatTime(history.savedAt)}"
            )
            
            _state.value = _state.value.copy(
                showRestoreDialog = false,
                selectedHistory = null
            )
            onRestored()
        }
    }
    
    fun deleteHistory() {
        val history = _state.value.selectedHistory ?: return
        
        viewModelScope.launch {
            contentHistoryRepository.deleteHistory(history.id)
            _state.value = _state.value.copy(
                showDeleteDialog = false,
                selectedHistory = null
            )
        }
    }
    
    fun deleteAllHistory(targetId: Long, targetType: String = "chapter") {
        viewModelScope.launch {
            contentHistoryRepository.deleteAllHistory(targetId, targetType)
            _state.value = _state.value.copy(
                showDeleteDialog = false,
                selectedHistory = null
            )
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }
}
