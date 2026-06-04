package com.bicy.novel.data.repository

import com.bicy.novel.data.local.dao.TimelineDao
import com.bicy.novel.data.local.entity.TimelineEntity
import com.bicy.novel.domain.model.TimelineEvent
import com.bicy.novel.domain.repository.TimelineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineRepositoryImpl @Inject constructor(
    private val timelineDao: TimelineDao
) : TimelineRepository {
    
    override fun getTimelineByNovelId(novelId: Long): Flow<List<TimelineEvent>> {
        return timelineDao.getTimelineByNovelId(novelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getTimelineByNovelIdOnce(novelId: Long): List<TimelineEvent> {
        return timelineDao.getTimelineByNovelIdOnce(novelId).map { it.toDomain() }
    }
    
    override suspend fun getTimelineById(id: Long): TimelineEvent? {
        return timelineDao.getTimelineById(id)?.toDomain()
    }
    
    override fun getKeyEvents(novelId: Long): Flow<List<TimelineEvent>> {
        return timelineDao.getKeyEvents(novelId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun createTimeline(event: TimelineEvent): Long {
        val maxOrder = timelineDao.getMaxSortOrder(event.novelId) ?: -1
        return timelineDao.insertTimeline(event.toEntity().copy(sortOrder = maxOrder + 1))
    }
    
    override suspend fun updateTimeline(event: TimelineEvent) {
        timelineDao.updateTimeline(event.toEntity())
    }
    
    override suspend fun deleteTimeline(event: TimelineEvent) {
        timelineDao.deleteTimeline(event.toEntity())
    }
    
    override suspend fun getKeyEventCount(novelId: Long): Int {
        return timelineDao.getKeyEventCount(novelId)
    }
    
    private fun TimelineEntity.toDomain() = TimelineEvent(
        id = id,
        novelId = novelId,
        title = title,
        description = description,
        eventDate = eventDate,
        sortOrder = sortOrder,
        isKeyEvent = isKeyEvent,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    private fun TimelineEvent.toEntity() = TimelineEntity(
        id = id,
        novelId = novelId,
        title = title,
        description = description,
        eventDate = eventDate,
        sortOrder = sortOrder,
        isKeyEvent = isKeyEvent,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
