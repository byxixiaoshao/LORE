package com.bicy.novel.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bicy.novel.data.ai.AIService
import com.bicy.novel.data.local.dao.AIOperationDao
import com.bicy.novel.data.local.dao.ContentHistoryDao
import com.bicy.novel.data.local.dao.DraftDao
import com.bicy.novel.data.local.database.AppDatabase
import com.bicy.novel.data.preferences.SettingsPreferences
import com.bicy.novel.domain.repository.ContentHistoryRepository
import com.bicy.novel.domain.repository.DraftRepository
import com.bicy.novel.data.repository.DraftRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
        }
    }
    
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS chat_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    novelId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (novelId) REFERENCES novels(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_sessions_novelId ON chat_sessions(novelId)")
            
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    FOREIGN KEY (sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId ON chat_messages(sessionId)")
        }
    }
    
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ai_operations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    messageId INTEGER NOT NULL,
                    operationType TEXT NOT NULL,
                    targetType TEXT NOT NULL,
                    targetId INTEGER NOT NULL,
                    targetName TEXT NOT NULL,
                    beforeData TEXT,
                    afterData TEXT,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_operations_sessionId ON ai_operations(sessionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_ai_operations_messageId ON ai_operations(messageId)")
        }
    }
    
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS chapter_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    chapterId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    wordCount INTEGER NOT NULL,
                    savedAt INTEGER NOT NULL,
                    note TEXT,
                    FOREIGN KEY (chapterId) REFERENCES chapters(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_history_chapterId ON chapter_history(chapterId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chapter_history_savedAt ON chapter_history(savedAt)")
        }
    }
    
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 删除旧表
            db.execSQL("DROP TABLE IF EXISTS chapter_history")
            // 创建新的通用历史表
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS content_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    targetType TEXT NOT NULL,
                    targetId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    wordCount INTEGER NOT NULL,
                    savedAt INTEGER NOT NULL,
                    note TEXT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_content_history_target ON content_history(targetId, targetType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_content_history_savedAt ON content_history(savedAt)")
        }
    }
    
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 创建写作统计表
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS writing_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    novelId INTEGER NOT NULL,
                    date INTEGER NOT NULL,
                    wordCount INTEGER NOT NULL,
                    wordsWritten INTEGER NOT NULL,
                    chaptersWritten INTEGER NOT NULL,
                    editDuration INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_writing_stats_novel_date ON writing_stats(novelId, date)")
        }
    }
    
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS drafts (
                    targetType TEXT NOT NULL,
                    targetId INTEGER NOT NULL,
                    novelId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    wordCount INTEGER NOT NULL,
                    savedAt INTEGER NOT NULL,
                    PRIMARY KEY(targetType, targetId)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_drafts_novelId ON drafts(novelId)")
        }
    }
    
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE novels ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
        }
    }
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "novel_editor_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideNovelDao(database: AppDatabase) = database.novelDao()
    
    @Provides
    @Singleton
    fun provideVolumeDao(database: AppDatabase) = database.volumeDao()
    
    @Provides
    @Singleton
    fun provideChapterDao(database: AppDatabase) = database.chapterDao()
    
    @Provides
    @Singleton
    fun provideCharacterDao(database: AppDatabase) = database.characterDao()
    
    @Provides
    @Singleton
    fun provideWorldviewDao(database: AppDatabase) = database.worldviewDao()
    
    @Provides
    @Singleton
    fun provideNoteDao(database: AppDatabase) = database.noteDao()
    
    @Provides
    @Singleton
    fun provideTimelineDao(database: AppDatabase) = database.timelineDao()
    
    @Provides
    @Singleton
    fun provideChatDao(database: AppDatabase) = database.chatDao()
    
    @Provides
    @Singleton
    fun provideAIOperationDao(database: AppDatabase) = database.aiOperationDao()
    
    @Provides
    @Singleton
    fun provideContentHistoryDao(database: AppDatabase) = database.contentHistoryDao()
    
    @Provides
    @Singleton
    fun provideWritingStatsDao(database: AppDatabase) = database.writingStatsDao()
    
    @Provides
    @Singleton
    fun provideContentHistoryRepository(
        contentHistoryDao: ContentHistoryDao,
        database: AppDatabase
    ): ContentHistoryRepository {
        return ContentHistoryRepository(contentHistoryDao, database)
    }
    
    @Provides
    @Singleton
    fun provideSettingsPreferences(
        @ApplicationContext context: Context
    ): SettingsPreferences {
        return SettingsPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideAIService(@ApplicationContext context: android.content.Context): AIService {
        return AIService(context)
    }
    
    @Provides
    @Singleton
    fun provideDraftDao(database: AppDatabase): DraftDao = database.draftDao()
    
    @Provides
    @Singleton
    fun provideDraftRepository(dao: DraftDao): DraftRepository {
        return DraftRepositoryImpl(dao)
    }
}
