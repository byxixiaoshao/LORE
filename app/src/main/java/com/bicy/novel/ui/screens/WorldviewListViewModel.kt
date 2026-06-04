package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Worldview
import com.bicy.novel.domain.repository.WorldviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorldviewListViewModel @Inject constructor(
    private val worldviewRepository: WorldviewRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    
    val worldviews = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) {
            worldviewRepository.getWorldviewsByNovelId(novelId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun setNovelId(id: Long) {
        _novelId.value = id
    }
    
    fun deleteWorldview(worldview: Worldview) {
        viewModelScope.launch {
            worldviewRepository.deleteWorldview(worldview)
        }
    }
}
