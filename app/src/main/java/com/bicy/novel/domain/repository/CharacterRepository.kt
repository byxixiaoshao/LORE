package com.bicy.novel.domain.repository

import com.bicy.novel.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun getCharactersByNovelId(novelId: Long): Flow<List<Character>>
    suspend fun getCharactersByNovelIdOnce(novelId: Long): List<Character>
    suspend fun getCharacterById(id: Long): Character?
    fun searchCharacters(novelId: Long, keyword: String): Flow<List<Character>>
    suspend fun createCharacter(character: Character): Long
    suspend fun updateCharacter(character: Character)
    suspend fun deleteCharacter(character: Character)
}
