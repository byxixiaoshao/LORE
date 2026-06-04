package com.bicy.novel.domain.repository

import com.bicy.novel.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow

interface TimelineRepository {
    fun getTimelineByNovelId(novelId: Long): Flow<List<TimelineEvent>>
    suspend fun getTimelineByNovelIdOnce(novelId: Long): List<TimelineEvent>
    suspend fun getTimelineById(id: Long): TimelineEvent?
    fun getKeyEvents(novelId: Long): Flow<List<TimelineEvent>>
    suspend fun createTimeline(event: TimelineEvent): Long
    suspend fun updateTimeline(event: TimelineEvent)
    suspend fun deleteTimeline(event: TimelineEvent)
    suspend fun getKeyEventCount(novelId: Long): Int
}
