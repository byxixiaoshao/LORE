package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.TimelineEvent
import com.bicy.novel.domain.repository.TimelineRepository
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
class TimelineViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    
    val events = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) {
            timelineRepository.getTimelineByNovelId(novelId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun setNovelId(id: Long) {
        _novelId.value = id
    }
    
    fun createEvent() {
        viewModelScope.launch {
            val id = _novelId.value
            if (id > 0) {
                timelineRepository.createTimeline(
                    TimelineEvent(
                        novelId = id,
                        title = "新事件"
                    )
                )
            }
        }
    }
    
    /**
     * 批量删除时间线事件
     * @param eventIds 要删除的事件ID列表
     */
    fun deleteEvents(eventIds: List<Long>) {
        viewModelScope.launch {
            eventIds.forEach { id ->
                val event = timelineRepository.getTimelineById(id)
                event?.let { timelineRepository.deleteTimeline(it) }
            }
        }
    }
}
