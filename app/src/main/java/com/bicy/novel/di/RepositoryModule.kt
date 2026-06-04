package com.bicy.novel.di

import com.bicy.novel.data.repository.*
import com.bicy.novel.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindNovelRepository(impl: NovelRepositoryImpl): NovelRepository
    
    @Binds
    @Singleton
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository
    
    @Binds
    @Singleton
    abstract fun bindCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository
    
    @Binds
    @Singleton
    abstract fun bindWorldviewRepository(impl: WorldviewRepositoryImpl): WorldviewRepository
    
    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
    
    @Binds
    @Singleton
    abstract fun bindTimelineRepository(impl: TimelineRepositoryImpl): TimelineRepository
    
    @Binds
    @Singleton
    abstract fun bindWritingStatsRepository(impl: WritingStatsRepositoryImpl): WritingStatsRepository
}
