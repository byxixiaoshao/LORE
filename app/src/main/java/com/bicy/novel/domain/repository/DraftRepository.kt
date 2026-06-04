package com.bicy.novel.domain.repository

import com.bicy.novel.domain.model.Draft
import kotlinx.coroutines.flow.Flow

interface DraftRepository {
    suspend fun saveDraft(draft: Draft)
    suspend fun getDraft(targetType: String, targetId: Long): Draft?
    suspend fun getDraftsByNovelId(novelId: Long): List<Draft>
    fun getDraftsByNovelIdFlow(novelId: Long): Flow<List<Draft>>
    suspend fun getDraftCount(novelId: Long): Int
    suspend fun deleteDraft(targetType: String, targetId: Long)
    suspend fun deleteDraftsByNovelId(novelId: Long)
}
