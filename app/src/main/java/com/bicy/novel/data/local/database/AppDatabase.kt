package com.bicy.novel.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bicy.novel.data.local.dao.*
import com.bicy.novel.data.local.entity.*

@Database(
    entities = [
        NovelEntity::class,
        VolumeEntity::class,
        ChapterEntity::class,
        CharacterEntity::class,
        WorldviewEntity::class,
        NoteEntity::class,
        TimelineEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        AIOperationEntity::class,
        ContentHistoryEntity::class,
        WritingStatsEntity::class,
        DraftEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun volumeDao(): VolumeDao
    abstract fun chapterDao(): ChapterDao
    abstract fun characterDao(): CharacterDao
    abstract fun worldviewDao(): WorldviewDao
    abstract fun noteDao(): NoteDao
    abstract fun timelineDao(): TimelineDao
    abstract fun chatDao(): ChatDao
    abstract fun aiOperationDao(): AIOperationDao
    abstract fun contentHistoryDao(): ContentHistoryDao
    abstract fun writingStatsDao(): WritingStatsDao
    abstract fun draftDao(): DraftDao
}
