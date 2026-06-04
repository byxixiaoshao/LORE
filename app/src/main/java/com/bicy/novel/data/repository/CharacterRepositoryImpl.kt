package com.bicy.novel.data.repository

import com.bicy.novel.data.local.dao.CharacterDao
import com.bicy.novel.data.local.entity.CharacterEntity
import com.bicy.novel.domain.model.Character
import com.bicy.novel.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val characterDao: CharacterDao
) : CharacterRepository {
    
    override fun getCharactersByNovelId(novelId: Long): Flow<List<Character>> {
        return characterDao.getCharactersByNovelId(novelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getCharactersByNovelIdOnce(novelId: Long): List<Character> {
        return characterDao.getCharactersByNovelIdOnce(novelId).map { it.toDomain() }
    }
    
    override suspend fun getCharacterById(id: Long): Character? {
        return characterDao.getCharacterById(id)?.toDomain()
    }
    
    override fun searchCharacters(novelId: Long, keyword: String): Flow<List<Character>> {
        return characterDao.searchCharacters(novelId, keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun createCharacter(character: Character): Long {
        return characterDao.insertCharacter(character.toEntity())
    }
    
    override suspend fun updateCharacter(character: Character) {
        characterDao.updateCharacter(character.toEntity())
    }
    
    override suspend fun deleteCharacter(character: Character) {
        characterDao.deleteCharacter(character.toEntity())
    }
    
    private fun CharacterEntity.toDomain() = Character(
        id = id,
        novelId = novelId,
        name = name,
        alias = alias,
        roleType = roleType,
        description = description,
        avatarPath = avatarPath,
        attributes = attributes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun Character.toEntity() = CharacterEntity(
        id = id,
        novelId = novelId,
        name = name,
        alias = alias,
        roleType = roleType,
        description = description,
        avatarPath = avatarPath,
        attributes = attributes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
