package com.bicy.novel.data.repository

import com.bicy.novel.data.local.dao.DraftDao
import com.bicy.novel.data.local.entity.DraftEntity
import com.bicy.novel.domain.model.Draft
import com.bicy.novel.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftRepositoryImpl @Inject constructor(
    private val dao: DraftDao
) : DraftRepository {
    
    override suspend fun saveDraft(draft: Draft) {
        dao.insertOrUpdate(
            DraftEntity(
                targetType = draft.targetType,
                targetId = draft.targetId,
                novelId = draft.novelId,
                title = draft.title,
                content = draft.content,
                wordCount = draft.wordCount,
                savedAt = draft.savedAt
            )
        )
    }
    
    override suspend fun getDraft(targetType: String, targetId: Long): Draft? {
        return dao.getDraft(targetType, targetId)?.toDomain()
    }
    
    override suspend fun getDraftsByNovelId(novelId: Long): List<Draft> {
        return dao.getDraftsByNovelId(novelId).map { it.toDomain() }
    }
    
    override fun getDraftsByNovelIdFlow(novelId: Long): Flow<List<Draft>> {
        return dao.getDraftsByNovelIdFlow(novelId).map { list -> list.map { it.toDomain() } }
    }
    
    override suspend fun getDraftCount(novelId: Long): Int {
        return dao.getDraftCount(novelId)
    }
    
    override suspend fun deleteDraft(targetType: String, targetId: Long) {
        dao.deleteDraft(targetType, targetId)
    }
    
    override suspend fun deleteDraftsByNovelId(novelId: Long) {
        dao.deleteDraftsByNovelId(novelId)
    }
    
    private fun DraftEntity.toDomain() = Draft(
        targetType = targetType,
        targetId = targetId,
        novelId = novelId,
        title = title,
        content = content,
        wordCount = wordCount,
        savedAt = savedAt
    )
}
