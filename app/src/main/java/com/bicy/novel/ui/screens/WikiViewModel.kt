package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Character
import com.bicy.novel.domain.model.Worldview
import com.bicy.novel.domain.repository.CharacterRepository
import com.bicy.novel.domain.repository.WorldviewRepository
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
class WikiViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    
    val characters = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) {
            characterRepository.getCharactersByNovelId(novelId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
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
    
    fun createCharacter() {
        viewModelScope.launch {
            val id = _novelId.value
            if (id > 0) {
                characterRepository.createCharacter(
                    Character(
                        novelId = id,
                        name = "新角色"
                    )
                )
            }
        }
    }
    
    fun createWorldview() {
        viewModelScope.launch {
            val id = _novelId.value
            if (id > 0) {
                worldviewRepository.createWorldview(
                    Worldview(
                        novelId = id,
                        title = "新设定"
                    )
                )
            }
        }
    }
    
    /**
     * 批量删除角色
     * @param characterIds 要删除的角色ID列表
     */
    fun deleteCharacters(characterIds: List<Long>) {
        viewModelScope.launch {
            characterIds.forEach { id ->
                val character = characterRepository.getCharacterById(id)
                character?.let { characterRepository.deleteCharacter(it) }
            }
        }
    }
    
    /**
     * 批量删除世界观
     * @param worldviewIds 要删除的世界观ID列表
     */
    fun deleteWorldviews(worldviewIds: List<Long>) {
        viewModelScope.launch {
            worldviewIds.forEach { id ->
                val worldview = worldviewRepository.getWorldviewById(id)
                worldview?.let { worldviewRepository.deleteWorldview(it) }
            }
        }
    }
}
