package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.TimelineEvent
import com.bicy.novel.domain.repository.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineEditViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository
) : ViewModel() {
    
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    
    private val _eventDate = MutableStateFlow("")
    val eventDate = _eventDate.asStateFlow()
    
    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    
    private val _isKeyEvent = MutableStateFlow(false)
    val isKeyEvent = _isKeyEvent.asStateFlow()
    
    private var eventId: Long? = null
    private var novelId: Long = 0
    
    fun init(novelIdFromNav: Long, eventIdFromNav: Long?) {
        novelId = novelIdFromNav
        eventId = eventIdFromNav
        
        eventIdFromNav?.let { id ->
            if (id > 0) {
                loadEvent(id)
            }
        }
    }
    
    private fun loadEvent(id: Long) {
        viewModelScope.launch {
            val event = timelineRepository.getTimelineById(id)
            event?.let {
                _title.value = it.title
                _eventDate.value = it.eventDate
                _description.value = it.description
                _isKeyEvent.value = it.isKeyEvent
                novelId = it.novelId
            }
        }
    }
    
    fun setTitle(value: String) { _title.value = value }
    fun setEventDate(value: String) { _eventDate.value = value }
    fun setDescription(value: String) { _description.value = value }
    fun setKeyEvent(value: Boolean) { _isKeyEvent.value = value }
    
    fun saveEvent(onSaved: () -> Unit) {
        if (_title.value.isBlank()) return
        
        viewModelScope.launch {
            val event = TimelineEvent(
                id = eventId ?: 0,
                novelId = novelId,
                title = _title.value,
                eventDate = _eventDate.value,
                description = _description.value,
                isKeyEvent = _isKeyEvent.value
            )
            
            if (eventId == null || eventId == 0L) {
                timelineRepository.createTimeline(event)
            } else {
                timelineRepository.updateTimeline(event)
            }
            onSaved()
        }
    }
}
