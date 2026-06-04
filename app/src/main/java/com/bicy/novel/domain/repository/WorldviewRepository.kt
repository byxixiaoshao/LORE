package com.bicy.novel.domain.repository

import com.bicy.novel.domain.model.Worldview
import kotlinx.coroutines.flow.Flow

interface WorldviewRepository {
    fun getWorldviewsByNovelId(novelId: Long): Flow<List<Worldview>>
    suspend fun getWorldviewsByNovelIdOnce(novelId: Long): List<Worldview>
    suspend fun getWorldviewById(id: Long): Worldview?
    fun getWorldviewsByCategory(novelId: Long, category: String): Flow<List<Worldview>>
    suspend fun createWorldview(worldview: Worldview): Long
    suspend fun updateWorldview(worldview: Worldview)
    suspend fun deleteWorldview(worldview: Worldview)
}
