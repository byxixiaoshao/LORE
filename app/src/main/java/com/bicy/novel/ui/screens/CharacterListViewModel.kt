package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Character
import com.bicy.novel.domain.repository.CharacterRepository
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
class CharacterListViewModel @Inject constructor(
    private val characterRepository: CharacterRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    
    val characters = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) {
            characterRepository.getCharactersByNovelId(novelId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun setNovelId(id: Long) {
        _novelId.value = id
    }
    
    fun deleteCharacter(character: Character) {
        viewModelScope.launch {
            characterRepository.deleteCharacter(character)
        }
    }
}
