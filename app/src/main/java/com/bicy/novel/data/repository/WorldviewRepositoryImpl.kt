package com.bicy.novel.data.repository

import com.bicy.novel.data.local.dao.WorldviewDao
import com.bicy.novel.data.local.entity.WorldviewEntity
import com.bicy.novel.domain.model.Worldview
import com.bicy.novel.domain.repository.WorldviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorldviewRepositoryImpl @Inject constructor(
    private val worldviewDao: WorldviewDao
) : WorldviewRepository {
    
    override fun getWorldviewsByNovelId(novelId: Long): Flow<List<Worldview>> {
        return worldviewDao.getWorldviewsByNovelId(novelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getWorldviewsByNovelIdOnce(novelId: Long): List<Worldview> {
        return worldviewDao.getWorldviewsByNovelIdOnce(novelId).map { it.toDomain() }
    }
    
    override suspend fun getWorldviewById(id: Long): Worldview? {
        return worldviewDao.getWorldviewById(id)?.toDomain()
    }
    
    override fun getWorldviewsByCategory(novelId: Long, category: String): Flow<List<Worldview>> {
        return worldviewDao.getWorldviewsByCategory(novelId, category).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun createWorldview(worldview: Worldview): Long {
        return worldviewDao.insertWorldview(worldview.toEntity())
    }
    
    override suspend fun updateWorldview(worldview: Worldview) {
        worldviewDao.updateWorldview(worldview.toEntity())
    }
    
    override suspend fun deleteWorldview(worldview: Worldview) {
        worldviewDao.deleteWorldview(worldview.toEntity())
    }
    
    private fun WorldviewEntity.toDomain() = Worldview(
        id = id,
        novelId = novelId,
        title = title,
        category = category,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun Worldview.toEntity() = WorldviewEntity(
        id = id,
        novelId = novelId,
        title = title,
        category = category,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
