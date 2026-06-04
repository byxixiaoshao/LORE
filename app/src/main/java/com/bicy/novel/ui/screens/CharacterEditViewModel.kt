package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Character
import com.bicy.novel.domain.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterEditViewModel @Inject constructor(
    private val characterRepository: CharacterRepository
) : ViewModel() {
    
    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()
    
    private val _alias = MutableStateFlow("")
    val alias = _alias.asStateFlow()
    
    private val _roleType = MutableStateFlow("")
    val roleType = _roleType.asStateFlow()
    
    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    
    private var characterId: Long? = null
    private var novelId: Long = 0
    
    fun init(novelIdFromNav: Long, characterIdFromNav: Long?) {
        novelId = novelIdFromNav
        characterId = characterIdFromNav
        
        characterIdFromNav?.let { id ->
            if (id > 0) {
                loadCharacter(id)
            }
        }
    }
    
    private fun loadCharacter(id: Long) {
        viewModelScope.launch {
            val character = characterRepository.getCharacterById(id)
            character?.let {
                _name.value = it.name
                _alias.value = it.alias
                _roleType.value = it.roleType
                _description.value = it.description
                novelId = it.novelId
            }
        }
    }
    
    fun setName(value: String) { _name.value = value }
    fun setAlias(value: String) { _alias.value = value }
    fun setRoleType(value: String) { _roleType.value = value }
    fun setDescription(value: String) { _description.value = value }
    
    fun saveCharacter(onSaved: () -> Unit) {
        if (_name.value.isBlank()) return
        
        viewModelScope.launch {
            val character = Character(
                id = characterId ?: 0,
                novelId = novelId,
                name = _name.value,
                alias = _alias.value,
                roleType = _roleType.value,
                description = _description.value
            )
            
            if (characterId == null || characterId == 0L) {
                characterRepository.createCharacter(character)
            } else {
                characterRepository.updateCharacter(character)
            }
            onSaved()
        }
    }
}
