package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.data.preferences.SettingsPreferences
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
import com.bicy.novel.domain.repository.NovelRepository
import com.bicy.novel.domain.repository.ContentHistoryRepository
import com.bicy.novel.domain.repository.WritingStatsRepository
import com.bicy.novel.domain.repository.DraftRepository
import com.bicy.novel.domain.model.Draft
import com.bicy.novel.util.WordCounter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EditType {
    CHAPTER,
    CHARACTER,
    WORLDVIEW,
    NOTE,
    TIMELINE
}

@HiltViewModel
class ChapterEditViewModel @Inject constructor(
    private val chapterRepository: ChapterRepository,
    private val novelRepository: NovelRepository,
    private val characterRepository: CharacterRepository,
    private val worldviewRepository: WorldviewRepository,
    private val noteRepository: NoteRepository,
    private val timelineRepository: TimelineRepository,
    private val settingsPreferences: SettingsPreferences,
    private val contentHistoryRepository: ContentHistoryRepository,
    private val writingStatsRepository: WritingStatsRepository,
    private val draftRepository: DraftRepository
) : ViewModel() {
    
    private val _showLineNumbers = MutableStateFlow(true)
    val showLineNumbers = _showLineNumbers.asStateFlow()
    
    private val _editorFontSize = MutableStateFlow(16)
    val editorFontSize = _editorFontSize.asStateFlow()
    
    private val _editorAutoWrap = MutableStateFlow(true)
    val editorAutoWrap = _editorAutoWrap.asStateFlow()
    
    private val _editType = MutableStateFlow(EditType.CHAPTER)
    val editType = _editType.asStateFlow()
    
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    
    private val _content = MutableStateFlow("")
    val content = _content.asStateFlow()
    
    private val _wordCount = MutableStateFlow(0)
    val wordCount = _wordCount.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    
    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()
    
    // 是否有草稿（用于红色显示，不触发自动保存）
    private val _hasDraft = MutableStateFlow(false)
    val hasDraft = _hasDraft.asStateFlow()
    
    private val _unsavedDraftCount = MutableStateFlow(0)
    val unsavedDraftCount = _unsavedDraftCount.asStateFlow()
    
    // 是否实际编辑过内容（控制自动保存）
    private var isDirty = false
    
    private val _sortOrder = MutableStateFlow(0)
    val sortOrder = _sortOrder.asStateFlow()
    
    var currentId: Long = 0
        private set
    private var novelId: Long = 0
    private var saveJob: Job? = null
    
    private val contentHistory = mutableListOf<String>()
    private var historyIndex = -1
    
    // 用于统计字数变化
    private var lastWordCount = 0
    private var sessionStartWordCount = 0
    
    // 用于检查保存时内容是否变化
    private var lastSavedTitle = ""
    private var lastSavedContent = ""
    
    init {
        viewModelScope.launch {
            settingsPreferences.settings.collect { settings ->
                _showLineNumbers.value = settings.editorShowLineNumbers
                _editorFontSize.value = settings.editorFontSize
                _editorAutoWrap.value = settings.editorAutoWrap
            }
        }
    }
    
    fun init(novelIdFromNav: Long, chapterIdFromNav: Long?) {
        novelId = novelIdFromNav
        currentId = chapterIdFromNav ?: 0
        _editType.value = EditType.CHAPTER
        
        viewModelScope.launch {
            _unsavedDraftCount.value = draftRepository.getDraftCount(novelIdFromNav)
        }
        
        chapterIdFromNav?.let { id ->
            if (id > 0) {
                loadChapter(id)
            }
        }
    }
    
    private fun loadChapter(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            isDirty = false
            val chapter = chapterRepository.getChapterById(id)
            chapter?.let {
                novelId = it.novelId
                val draft = draftRepository.getDraft("chapter", id)
                if (draft != null) {
                    _title.value = draft.title
                    _content.value = draft.content
                    _wordCount.value = draft.wordCount
                    _hasDraft.value = true
                } else {
                    _title.value = it.title
                    _content.value = it.content
                    _wordCount.value = it.wordCount
                    _hasDraft.value = false
                }
                _hasUnsavedChanges.value = false
                _sortOrder.value = it.sortOrder
                resetHistory(_content.value)
                lastWordCount = it.wordCount
                sessionStartWordCount = it.wordCount
                // 记录加载时的内容，用于保存时比较
                lastSavedTitle = it.title
                lastSavedContent = it.content
            }
            _isLoading.value = false
        }
    }
    
    fun reloadChapter(chapterId: Long) {
        loadChapter(chapterId)
    }
    
    /**
     * AI写入后重新加载，跳过草稿（因为AI直接写入数据库）
     */
    fun reloadFromDatabase(chapterId: Long) {
        loadFromDatabase(chapterId)
    }
    
    private fun loadFromDatabase(chapterId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            when (_editType.value) {
                EditType.CHAPTER -> {
                    val chapter = chapterRepository.getChapterById(chapterId)
                    chapter?.let {
                        _title.value = it.title
                        _content.value = it.content
                        _wordCount.value = it.wordCount
                        _sortOrder.value = it.sortOrder
                        novelId = it.novelId
                        _hasUnsavedChanges.value = false
                        resetHistory(it.content)
                        lastWordCount = it.wordCount
                        sessionStartWordCount = it.wordCount
                        // 清除草稿
                        draftRepository.deleteDraft("chapter", chapterId)
                    }
                }
                EditType.CHARACTER -> {
                    val character = characterRepository.getCharacterById(chapterId)
                    character?.let {
                        _title.value = it.name
                        _content.value = it.description
                        _wordCount.value = WordCounter.count(it.description)
                        novelId = it.novelId
                        _hasUnsavedChanges.value = false
                        resetHistory(it.description)
                        draftRepository.deleteDraft("character", chapterId)
                    }
                }
                EditType.WORLDVIEW -> {
                    val worldview = worldviewRepository.getWorldviewById(chapterId)
                    worldview?.let {
                        _title.value = it.title
                        _content.value = it.content
                        _wordCount.value = WordCounter.count(it.content)
                        novelId = it.novelId
                        _hasUnsavedChanges.value = false
                        resetHistory(it.content)
                        draftRepository.deleteDraft("worldview", chapterId)
                    }
                }
                EditType.NOTE -> {
                    val note = noteRepository.getNoteById(chapterId)
                    note?.let {
                        _title.value = it.title
                        _content.value = it.content
                        _wordCount.value = WordCounter.count(it.content)
                        it.novelId?.let { nid -> novelId = nid }
                        _hasUnsavedChanges.value = false
                        resetHistory(it.content)
                        draftRepository.deleteDraft("note", chapterId)
                    }
                }
                EditType.TIMELINE -> {
                    val event = timelineRepository.getTimelineById(chapterId)
                    event?.let {
                        _title.value = it.title
                        _content.value = it.description
                        _wordCount.value = WordCounter.count(it.description)
                        novelId = it.novelId
                        _hasUnsavedChanges.value = false
                        resetHistory(it.description)
                        draftRepository.deleteDraft("timeline", chapterId)
                    }
                }
            }
            _unsavedDraftCount.value = draftRepository.getDraftCount(novelId)
            _isLoading.value = false
        }
    }
    
    private fun loadCharacter(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            isDirty = false
            val character = characterRepository.getCharacterById(id)
            character?.let {
                novelId = it.novelId
                val draft = draftRepository.getDraft("character", id)
                if (draft != null) {
                    _title.value = draft.title
                    _content.value = draft.content
                    _wordCount.value = draft.wordCount
                    _hasDraft.value = true
                } else {
                    _title.value = it.name
                    _content.value = it.description
                    _wordCount.value = WordCounter.count(it.description)
                    _hasDraft.value = false
                }
                _hasUnsavedChanges.value = false
                resetHistory(_content.value)
                lastSavedTitle = it.name
                lastSavedContent = it.description
            }
            _isLoading.value = false
        }
    }
    
    private fun loadWorldview(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            isDirty = false
            val worldview = worldviewRepository.getWorldviewById(id)
            worldview?.let {
                novelId = it.novelId
                val draft = draftRepository.getDraft("worldview", id)
                if (draft != null) {
                    _title.value = draft.title
                    _content.value = draft.content
                    _wordCount.value = draft.wordCount
                    _hasDraft.value = true
                } else {
                    _title.value = it.title
                    _content.value = it.content
                    _wordCount.value = WordCounter.count(it.content)
                    _hasDraft.value = false
                }
                _hasUnsavedChanges.value = false
                resetHistory(_content.value)
                lastSavedTitle = it.title
                lastSavedContent = it.content
            }
            _isLoading.value = false
        }
    }
    
    private fun loadNote(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            isDirty = false
            val note = noteRepository.getNoteById(id)
            note?.let {
                it.novelId?.let { nid -> novelId = nid }
                val draft = draftRepository.getDraft("note", id)
                if (draft != null) {
                    _title.value = draft.title
                    _content.value = draft.content
                    _wordCount.value = draft.wordCount
                    _hasDraft.value = true
                } else {
                    _title.value = it.title
                    _content.value = it.content
                    _wordCount.value = WordCounter.count(it.content)
                    _hasDraft.value = false
                }
                _hasUnsavedChanges.value = false
                resetHistory(_content.value)
                lastSavedTitle = it.title
                lastSavedContent = it.content
            }
            _isLoading.value = false
        }
    }
    
    private fun loadTimeline(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            isDirty = false
            val event = timelineRepository.getTimelineById(id)
            event?.let {
                novelId = it.novelId
                val draft = draftRepository.getDraft("timeline", id)
                if (draft != null) {
                    _title.value = draft.title
                    _content.value = draft.content
                    _wordCount.value = draft.wordCount
                    _hasDraft.value = true
                } else {
                    _title.value = it.title
                    _content.value = it.description
                    _wordCount.value = WordCounter.count(it.description)
                    _hasDraft.value = false
                }
                _hasUnsavedChanges.value = false
                resetHistory(_content.value)
                lastSavedTitle = it.title
                lastSavedContent = it.description
            }
            _isLoading.value = false
        }
    }
    
    private fun resetHistory(content: String) {
        contentHistory.clear()
        contentHistory.add(content)
        historyIndex = 0
    }
    
    fun setTitle(value: String) {
        _title.value = value
        _hasUnsavedChanges.value = true
        isDirty = true
        scheduleAutoSave()
    }
    
    fun setSortOrder(value: Int) {
        _sortOrder.value = value
        _hasUnsavedChanges.value = true
        isDirty = true
        scheduleAutoSave()
    }
    
    fun setContent(value: String) {
        _content.value = value
        _wordCount.value = WordCounter.count(value)
        _hasUnsavedChanges.value = true
        isDirty = true
        
        if (historyIndex == contentHistory.lastIndex) {
            contentHistory.add(value)
            if (contentHistory.size > 100) {
                contentHistory.removeAt(0)
            }
        } else {
            contentHistory.subList(historyIndex + 1, contentHistory.size).clear()
            contentHistory.add(value)
        }
        historyIndex = contentHistory.lastIndex
        
        scheduleAutoSave()
    }
    
    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            _content.value = contentHistory[historyIndex]
            _wordCount.value = WordCounter.count(_content.value)
            _hasUnsavedChanges.value = true
            isDirty = true
            scheduleAutoSave()
        }
    }
    
    fun redo() {
        if (historyIndex < contentHistory.lastIndex) {
            historyIndex++
            _content.value = contentHistory[historyIndex]
            _wordCount.value = WordCounter.count(_content.value)
            _hasUnsavedChanges.value = true
            isDirty = true
            scheduleAutoSave()
        }
    }
    
    private fun scheduleAutoSave() {
        if (!isDirty) return
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(3000)
            saveDraft()
        }
    }
    
    private fun targetType(): String = when (_editType.value) {
        EditType.CHAPTER -> "chapter"
        EditType.CHARACTER -> "character"
        EditType.WORLDVIEW -> "worldview"
        EditType.NOTE -> "note"
        EditType.TIMELINE -> "timeline"
    }
    
    private suspend fun saveDraft() {
        if (_title.value.isBlank()) return
        
        draftRepository.saveDraft(
            Draft(
                targetType = targetType(),
                targetId = currentId,
                novelId = novelId,
                title = _title.value,
                content = _content.value,
                wordCount = _wordCount.value
            )
        )
        _unsavedDraftCount.value = draftRepository.getDraftCount(novelId)
    }
    
    private suspend fun saveSuspend() {
        if (_title.value.isBlank()) return
        
        _isSaving.value = true
        
        when (_editType.value) {
            EditType.CHAPTER -> saveChapterContent()
            EditType.CHARACTER -> saveCharacterContent()
            EditType.WORLDVIEW -> saveWorldviewContent()
            EditType.NOTE -> saveNoteContent()
            EditType.TIMELINE -> saveTimelineContent()
        }
        
        if (_editType.value == EditType.CHAPTER) {
            updateNovelStats()
        }
        
        _hasUnsavedChanges.value = false
        _isSaving.value = false
    }
    
    fun saveChapter(onSaved: (() -> Unit)? = null) {
        if (_title.value.isBlank()) return
        
        viewModelScope.launch {
            saveSuspend()
            // 正式保存后清除草稿
            draftRepository.deleteDraft(targetType(), currentId)
            _hasUnsavedChanges.value = false
            _hasDraft.value = false
            isDirty = false
            _unsavedDraftCount.value = draftRepository.getDraftCount(novelId)
            onSaved?.invoke()
        }
    }
    
    private suspend fun saveChapterContent() {
        val isNewChapter = currentId == 0L
        
        val sortOrderToUse = if (isNewChapter && _sortOrder.value == 0) {
            chapterRepository.getChapterCount(novelId) + 1
        } else {
            _sortOrder.value
        }
        
        val chapter = Chapter(
            id = currentId,
            novelId = novelId,
            title = _title.value,
            content = _content.value,
            wordCount = _wordCount.value,
            sortOrder = sortOrderToUse
        )
        
        if (isNewChapter) {
            val newId = chapterRepository.createChapter(chapter)
            currentId = newId
            _sortOrder.value = sortOrderToUse
            
            // 记录新章节的统计
            if (_wordCount.value > 0) {
                writingStatsRepository.recordStats(
                    novelId = novelId,
                    wordsWritten = _wordCount.value,
                    chaptersWritten = 1
                )
            }
        } else {
            chapterRepository.updateChapter(chapter)
            
            // 保存历史记录（仅更新时，且内容有变化）
            val titleChanged = _title.value != lastSavedTitle
            val contentChanged = _content.value != lastSavedContent
            if (titleChanged || contentChanged) {
                contentHistoryRepository.saveHistory(
                    targetType = "chapter",
                    targetId = currentId,
                    title = _title.value,
                    content = _content.value,
                    wordCount = _wordCount.value
                )
                lastSavedTitle = _title.value
                lastSavedContent = _content.value
            }
            
            // 记录字数变化统计
            val wordDiff = _wordCount.value - lastWordCount
            if (wordDiff != 0) {
                writingStatsRepository.recordStats(
                    novelId = novelId,
                    wordsWritten = if (wordDiff > 0) wordDiff else 0,
                    chaptersWritten = 1
                )
                lastWordCount = _wordCount.value
            }
        }
    }
    
    private suspend fun saveCharacterContent() {
        val character = Character(
            id = currentId,
            novelId = novelId,
            name = _title.value,
            description = _content.value
        )
        
        if (currentId == 0L) {
            val newId = characterRepository.createCharacter(character)
            currentId = newId
        } else {
            characterRepository.updateCharacter(character)
            
            // 保存历史记录（仅内容有变化时）
            if (_title.value != lastSavedTitle || _content.value != lastSavedContent) {
                contentHistoryRepository.saveHistory(
                    targetType = "character",
                    targetId = currentId,
                    title = _title.value,
                    content = _content.value,
                    wordCount = _wordCount.value
                )
                lastSavedTitle = _title.value
                lastSavedContent = _content.value
            }
        }
    }
    
    private suspend fun saveWorldviewContent() {
        val worldview = Worldview(
            id = currentId,
            novelId = novelId,
            title = _title.value,
            content = _content.value
        )
        
        if (currentId == 0L) {
            val newId = worldviewRepository.createWorldview(worldview)
            currentId = newId
        } else {
            worldviewRepository.updateWorldview(worldview)
            
            // 保存历史记录（仅内容有变化时）
            if (_title.value != lastSavedTitle || _content.value != lastSavedContent) {
                contentHistoryRepository.saveHistory(
                    targetType = "worldview",
                    targetId = currentId,
                    title = _title.value,
                    content = _content.value,
                    wordCount = _wordCount.value
                )
                lastSavedTitle = _title.value
                lastSavedContent = _content.value
            }
        }
    }
    
    private suspend fun saveNoteContent() {
        val note = Note(
            id = currentId,
            novelId = novelId,
            title = _title.value,
            content = _content.value
        )
        
        if (currentId == 0L) {
            val newId = noteRepository.createNote(note)
            currentId = newId
        } else {
            noteRepository.updateNote(note)
            
            // 保存历史记录（仅内容有变化时）
            if (_title.value != lastSavedTitle || _content.value != lastSavedContent) {
                contentHistoryRepository.saveHistory(
                    targetType = "note",
                    targetId = currentId,
                    title = _title.value,
                    content = _content.value,
                    wordCount = _wordCount.value
                )
                lastSavedTitle = _title.value
                lastSavedContent = _content.value
            }
        }
    }
    
    private suspend fun saveTimelineContent() {
        val event = TimelineEvent(
            id = currentId,
            novelId = novelId,
            title = _title.value,
            description = _content.value
        )
        
        if (currentId == 0L) {
            val newId = timelineRepository.createTimeline(event)
            currentId = newId
        } else {
            timelineRepository.updateTimeline(event)
            
            // 保存历史记录（仅内容有变化时）
            if (_title.value != lastSavedTitle || _content.value != lastSavedContent) {
                contentHistoryRepository.saveHistory(
                    targetType = "timeline",
                    targetId = currentId,
                    title = _title.value,
                    content = _content.value,
                    wordCount = _wordCount.value
                )
                lastSavedTitle = _title.value
                lastSavedContent = _content.value
            }
        }
    }
    
    private suspend fun updateNovelStats() {
        val wordCount = chapterRepository.getTotalWordCount(novelId)
        val chapterCount = chapterRepository.getChapterCount(novelId)
        novelRepository.updateNovelStats(novelId, wordCount, chapterCount)
    }
    
    fun switchToChapter(newId: Long) {
        viewModelScope.launch {
            saveIfHasChanges()
            _editType.value = EditType.CHAPTER
            currentId = newId
            loadChapter(newId)
        }
    }
    
    fun switchToCharacter(newId: Long) {
        viewModelScope.launch {
            saveIfHasChanges()
            _editType.value = EditType.CHARACTER
            currentId = newId
            loadCharacter(newId)
        }
    }
    
    fun switchToWorldview(newId: Long) {
        viewModelScope.launch {
            saveIfHasChanges()
            _editType.value = EditType.WORLDVIEW
            currentId = newId
            loadWorldview(newId)
        }
    }
    
    fun switchToNote(newId: Long) {
        viewModelScope.launch {
            saveIfHasChanges()
            _editType.value = EditType.NOTE
            currentId = newId
            loadNote(newId)
        }
    }
    
    fun switchToTimeline(newId: Long) {
        viewModelScope.launch {
            saveIfHasChanges()
            _editType.value = EditType.TIMELINE
            currentId = newId
            loadTimeline(newId)
        }
    }
    
    private suspend fun saveIfHasChanges() {
        if (_title.value.isNotBlank() && isDirty) {
            saveDraft()
        }
    }
    
    /**
     * 获取所有未保存的草稿列表（用于退出对话框）
     */
    suspend fun getUnsavedDrafts(): List<Draft> {
        return draftRepository.getDraftsByNovelId(novelId)
    }
    
    /**
     * 保存所有草稿到正式存储
     */
    suspend fun saveAllDrafts() {
        val drafts = draftRepository.getDraftsByNovelId(novelId)
        for (draft in drafts) {
            when (draft.targetType) {
                "chapter" -> saveDraftAsChapter(draft)
                "character" -> saveDraftAsCharacter(draft)
                "worldview" -> saveDraftAsWorldview(draft)
                "note" -> saveDraftAsNote(draft)
                "timeline" -> saveDraftAsTimeline(draft)
            }
            draftRepository.deleteDraft(draft.targetType, draft.targetId)
        }
        _unsavedDraftCount.value = 0
    }
    
    private suspend fun saveDraftAsChapter(draft: Draft) {
        val chapter = Chapter(
            id = draft.targetId,
            novelId = novelId,
            title = draft.title,
            content = draft.content,
            wordCount = draft.wordCount
        )
        if (draft.targetId == 0L) {
            val newId = chapterRepository.createChapter(chapter.copy(sortOrder = chapterRepository.getChapterCount(novelId) + 1))
            if (_editType.value == EditType.CHAPTER && currentId == 0L) currentId = newId
        } else {
            chapterRepository.updateChapter(chapter)
            contentHistoryRepository.saveHistory("chapter", draft.targetId, draft.title, draft.content, draft.wordCount)
        }
    }
    
    private suspend fun saveDraftAsCharacter(draft: Draft) {
        val character = Character(
            id = draft.targetId,
            novelId = novelId,
            name = draft.title,
            description = draft.content
        )
        if (draft.targetId == 0L) {
            characterRepository.createCharacter(character)
        } else {
            characterRepository.updateCharacter(character)
            contentHistoryRepository.saveHistory("character", draft.targetId, draft.title, draft.content, draft.wordCount)
        }
    }
    
    private suspend fun saveDraftAsWorldview(draft: Draft) {
        val worldview = Worldview(
            id = draft.targetId,
            novelId = novelId,
            title = draft.title,
            content = draft.content
        )
        if (draft.targetId == 0L) {
            worldviewRepository.createWorldview(worldview)
        } else {
            worldviewRepository.updateWorldview(worldview)
            contentHistoryRepository.saveHistory("worldview", draft.targetId, draft.title, draft.content, draft.wordCount)
        }
    }
    
    private suspend fun saveDraftAsNote(draft: Draft) {
        val note = Note(
            id = draft.targetId,
            novelId = novelId,
            title = draft.title,
            content = draft.content
        )
        if (draft.targetId == 0L) {
            noteRepository.createNote(note)
        } else {
            noteRepository.updateNote(note)
            contentHistoryRepository.saveHistory("note", draft.targetId, draft.title, draft.content, draft.wordCount)
        }
    }
    
    private suspend fun saveDraftAsTimeline(draft: Draft) {
        val event = TimelineEvent(
            id = draft.targetId,
            novelId = novelId,
            title = draft.title,
            description = draft.content
        )
        if (draft.targetId == 0L) {
            timelineRepository.createTimeline(event)
        } else {
            timelineRepository.updateTimeline(event)
            contentHistoryRepository.saveHistory("timeline", draft.targetId, draft.title, draft.content, draft.wordCount)
        }
    }
    
    /**
     * 批量查找替换
     * @param searchText 查找文本
     * @param replaceText 替换文本
     * @param scope 替换范围
     * @return 替换结果信息
     */
    suspend fun batchFindReplace(
        searchText: String,
        replaceText: String,
        scope: com.bicy.novel.ui.components.ReplaceScope
    ): String {
        if (searchText.isEmpty()) return "查找内容不能为空"
        
        var totalReplaced = 0
        
        when (scope) {
            com.bicy.novel.ui.components.ReplaceScope.CURRENT_CHAPTER -> {
                // 当前章节替换
                val currentContent = _content.value
                val count = countOccurrences(currentContent, searchText)
                if (count > 0) {
                    _content.value = currentContent.replace(searchText, replaceText)
                    _wordCount.value = WordCounter.count(_content.value)
                    totalReplaced = count
                    _hasUnsavedChanges.value = true
                    scheduleAutoSave()
                }
            }
            com.bicy.novel.ui.components.ReplaceScope.ALL_CHAPTERS -> {
                // 所有章节替换
                val chapters = chapterRepository.getChaptersByNovelIdOnce(novelId)
                chapters.forEach { chapter ->
                    val count = countOccurrences(chapter.content, searchText)
                    if (count > 0) {
                        val newContent = chapter.content.replace(searchText, replaceText)
                        val newWordCount = WordCounter.count(newContent)
                        chapterRepository.updateChapter(
                            chapter.copy(
                                content = newContent,
                                wordCount = newWordCount
                            )
                        )
                        totalReplaced += count
                        
                        // 如果是当前编辑的章节，更新UI
                        if (chapter.id == currentId) {
                            _content.value = newContent
                            _wordCount.value = newWordCount
                            _hasUnsavedChanges.value = true
                        }
                    }
                }
                if (totalReplaced > 0) {
                    scheduleAutoSave()
                }
            }
            com.bicy.novel.ui.components.ReplaceScope.ALL_CONTENT -> {
                // 所有内容替换（章节、角色、世界观、笔记、时间线）
                
                // 章节
                val chapters = chapterRepository.getChaptersByNovelIdOnce(novelId)
                chapters.forEach { chapter ->
                    val count = countOccurrences(chapter.content, searchText)
                    if (count > 0) {
                        val newContent = chapter.content.replace(searchText, replaceText)
                        val newWordCount = WordCounter.count(newContent)
                        chapterRepository.updateChapter(
                            chapter.copy(
                                content = newContent,
                                wordCount = newWordCount
                            )
                        )
                        totalReplaced += count
                        
                        if (chapter.id == currentId) {
                            _content.value = newContent
                            _wordCount.value = newWordCount
                            _hasUnsavedChanges.value = true
                        }
                    }
                }
                
                // 角色
                val characters = characterRepository.getCharactersByNovelIdOnce(novelId)
                characters.forEach { character ->
                    val count = countOccurrences(character.description, searchText)
                    if (count > 0) {
                        val newDescription = character.description.replace(searchText, replaceText)
                        characterRepository.updateCharacter(
                            character.copy(description = newDescription)
                        )
                        totalReplaced += count
                    }
                }
                
                // 世界观
                val worldviews = worldviewRepository.getWorldviewsByNovelIdOnce(novelId)
                worldviews.forEach { worldview ->
                    val count = countOccurrences(worldview.content, searchText)
                    if (count > 0) {
                        val newContent = worldview.content.replace(searchText, replaceText)
                        worldviewRepository.updateWorldview(
                            worldview.copy(content = newContent)
                        )
                        totalReplaced += count
                    }
                }
                
                // 笔记
                val notes = noteRepository.getNotesByNovelIdOnce(novelId)
                notes.forEach { note ->
                    val count = countOccurrences(note.content, searchText)
                    if (count > 0) {
                        val newContent = note.content.replace(searchText, replaceText)
                        noteRepository.updateNote(
                            note.copy(content = newContent)
                        )
                        totalReplaced += count
                    }
                }
                
                // 时间线
                val events = timelineRepository.getTimelineByNovelIdOnce(novelId)
                events.forEach { event ->
                    val count = countOccurrences(event.description, searchText)
                    if (count > 0) {
                        val newDescription = event.description.replace(searchText, replaceText)
                        timelineRepository.updateTimeline(
                            event.copy(description = newDescription)
                        )
                        totalReplaced += count
                    }
                }
                
                if (totalReplaced > 0) {
                    scheduleAutoSave()
                }
            }
        }
        
        return if (totalReplaced > 0) {
            "成功替换 $totalReplaced 处"
        } else {
            "未找到匹配内容"
        }
    }
    
    /**
     * 计算文本中出现次数
     */
    private fun countOccurrences(text: String, search: String): Int {
        var count = 0
        var index = text.indexOf(search)
        while (index >= 0) {
            count++
            index = text.indexOf(search, index + 1)
        }
        return count
    }
}
