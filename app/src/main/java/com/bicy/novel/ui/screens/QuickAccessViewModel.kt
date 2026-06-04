package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.domain.model.Chapter
import com.bicy.novel.domain.model.Character
import com.bicy.novel.domain.model.Worldview
import com.bicy.novel.domain.model.Note
import com.bicy.novel.domain.model.TimelineEvent
import com.bicy.novel.domain.repository.ChapterRepository
import com.bicy.novel.domain.repository.CharacterRepository
import com.bicy.novel.domain.repository.WorldviewRepository
import com.bicy.novel.domain.repository.NoteRepository
import com.bicy.novel.domain.repository.TimelineRepository
import com.bicy.novel.domain.repository.DraftRepository
import com.bicy.novel.domain.model.Draft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class QuickAccessViewModel @Inject constructor(
    private val chapterRepository: ChapterRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository,
    private val draftRepository: DraftRepository
) : ViewModel() {
    
    private val _novelId = MutableStateFlow(0L)
    
    val drafts = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) draftRepository.getDraftsByNovelIdFlow(novelId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val chapters = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) chapterRepository.getChaptersByNovelId(novelId).map { list -> list.sortedBy { it.sortOrder } }
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val characters = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) characterRepository.getCharactersByNovelId(novelId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val worldviews = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) worldviewRepository.getWorldviewsByNovelId(novelId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val notes = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) noteRepository.getNotesByNovelId(novelId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val timelineEvents = _novelId.flatMapLatest { novelId ->
        if (novelId > 0) timelineRepository.getTimelineByNovelId(novelId)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun loadNovelId(id: Long) {
        _novelId.value = id
    }
    
    fun deleteItem(section: QuickNavSection, itemId: Long) {
        viewModelScope.launch {
            when (section) {
                QuickNavSection.CONTENT -> chapterRepository.deleteChapter(itemId)
                QuickNavSection.CHARACTER -> {
                    characterRepository.getCharacterById(itemId)?.let { character ->
                        characterRepository.deleteCharacter(character)
                    }
                }
                QuickNavSection.WORLDVIEW -> {
                    worldviewRepository.getWorldviewById(itemId)?.let { worldview ->
                        worldviewRepository.deleteWorldview(worldview)
                    }
                }
                QuickNavSection.NOTE -> {
                    noteRepository.getNoteById(itemId)?.let { note ->
                        noteRepository.deleteNote(note)
                    }
                }
                QuickNavSection.TIMELINE -> {
                    timelineRepository.getTimelineById(itemId)?.let { event ->
                        timelineRepository.deleteTimeline(event)
                    }
                }
                QuickNavSection.AI -> {}
            }
        }
    }
    
    fun renameItem(section: QuickNavSection, itemId: Long, newName: String, newSortOrder: Int? = null) {
        viewModelScope.launch {
            when (section) {
                QuickNavSection.CONTENT -> {
                    chapterRepository.getChapterById(itemId)?.let { chapter ->
                        chapterRepository.updateChapter(chapter.copy(title = newName, sortOrder = newSortOrder ?: chapter.sortOrder))
                    }
                }
                QuickNavSection.CHARACTER -> {
                    characterRepository.getCharacterById(itemId)?.let { character ->
                        characterRepository.updateCharacter(character.copy(name = newName))
                    }
                }
                QuickNavSection.WORLDVIEW -> {
                    worldviewRepository.getWorldviewById(itemId)?.let { worldview ->
                        worldviewRepository.updateWorldview(worldview.copy(title = newName))
                    }
                }
                QuickNavSection.NOTE -> {
                    noteRepository.getNoteById(itemId)?.let { note ->
                        noteRepository.updateNote(note.copy(title = newName))
                    }
                }
                QuickNavSection.TIMELINE -> {
                    timelineRepository.getTimelineById(itemId)?.let { event ->
                        timelineRepository.updateTimeline(event.copy(title = newName))
                    }
                }
                QuickNavSection.AI -> {}
            }
        }
    }
    
    fun moveChapterUp(chapterId: Long) {
        viewModelScope.launch {
            val chapters = chapterRepository.getChaptersByNovelIdOnce(_novelId.value).sortedBy { it.sortOrder }
            val currentIndex = chapters.indexOfFirst { it.id == chapterId }
            if (currentIndex > 0) {
                val mutableList = chapters.toMutableList()
                val currentChapter = mutableList.removeAt(currentIndex)
                mutableList.add(currentIndex - 1, currentChapter)
                
                mutableList.forEachIndexed { index, chapter ->
                    if (chapter.sortOrder != index + 1) {
                        chapterRepository.updateChapter(chapter.copy(sortOrder = index + 1))
                    }
                }
            }
        }
    }
    
    fun moveChapterDown(chapterId: Long) {
        viewModelScope.launch {
            val chapters = chapterRepository.getChaptersByNovelIdOnce(_novelId.value).sortedBy { it.sortOrder }
            val currentIndex = chapters.indexOfFirst { it.id == chapterId }
            if (currentIndex < chapters.size - 1) {
                val mutableList = chapters.toMutableList()
                val currentChapter = mutableList.removeAt(currentIndex)
                mutableList.add(currentIndex + 1, currentChapter)
                
                mutableList.forEachIndexed { index, chapter ->
                    if (chapter.sortOrder != index + 1) {
                        chapterRepository.updateChapter(chapter.copy(sortOrder = index + 1))
                    }
                }
            }
        }
    }
    
    suspend fun createNewChapter(title: String): Long {
        val count = chapterRepository.getChapterCount(_novelId.value)
        val chapter = Chapter(
            novelId = _novelId.value,
            title = title,
            content = "",
            wordCount = 0,
            sortOrder = count + 1
        )
        return chapterRepository.createChapter(chapter)
    }
    
    suspend fun createNewCharacter(name: String): Long {
        val character = Character(
            novelId = _novelId.value,
            name = name,
            description = ""
        )
        return characterRepository.createCharacter(character)
    }
    
    suspend fun createNewWorldview(title: String): Long {
        val worldview = Worldview(
            novelId = _novelId.value,
            title = title,
            content = ""
        )
        return worldviewRepository.createWorldview(worldview)
    }
    
    suspend fun createNewNote(title: String): Long {
        val note = Note(
            novelId = _novelId.value,
            title = title,
            content = ""
        )
        return noteRepository.createNote(note)
    }
    
    suspend fun createNewTimeline(title: String): Long {
        val event = TimelineEvent(
            novelId = _novelId.value,
            title = title,
            description = ""
        )
        return timelineRepository.createTimeline(event)
    }
}
