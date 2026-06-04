package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Worldview
import com.bicy.novel.domain.repository.WorldviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorldviewEditViewModel @Inject constructor(
    private val worldviewRepository: WorldviewRepository
) : ViewModel() {
    
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    
    private val _category = MutableStateFlow("")
    val category = _category.asStateFlow()
    
    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()
    
    private var worldviewId: Long? = null
    private var novelId: Long = 0
    
    fun init(novelIdFromNav: Long, worldviewIdFromNav: Long?) {
        novelId = novelIdFromNav
        worldviewId = worldviewIdFromNav
        
        worldviewIdFromNav?.let { id ->
            if (id > 0) {
                loadWorldview(id)
            }
        }
    }
    
    private fun loadWorldview(id: Long) {
        viewModelScope.launch {
            val worldview = worldviewRepository.getWorldviewById(id)
            worldview?.let {
                _title.value = it.title
                _category.value = it.category
                _content.value = it.content
                novelId = it.novelId
            }
        }
    }
    
    fun setTitle(value: String) { _title.value = value }
    fun setCategory(value: String) { _category.value = value }
    fun setContent(value: String) { _content.value = value }
    
    fun saveWorldview(onSaved: () -> Unit) {
        if (_title.value.isBlank()) return
        
        viewModelScope.launch {
            val worldview = Worldview(
                id = worldviewId ?: 0,
                novelId = novelId,
                title = _title.value,
                category = _category.value,
                content = _content.value
            )
            
            if (worldviewId == null || worldviewId == 0L) {
                worldviewRepository.createWorldview(worldview)
            } else {
                worldviewRepository.updateWorldview(worldview)
            }
            onSaved()
        }
    }
}
