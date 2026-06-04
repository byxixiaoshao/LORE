package com.bicy.novel.ui.navigation

sealed class Screen(val route: String) {
    object NovelList : Screen("novel_list")
    object Settings : Screen("settings")
    object LogViewer : Screen("log_viewer")
    
    object NovelDetail : Screen("novel_detail/{novelId}") {
        fun createRoute(novelId: Long) = "novel_detail/$novelId"
    }
    
    object NovelEdit : Screen("novel_edit/{novelId}") {
        fun createRoute(novelId: Long) = "novel_edit/$novelId"
        const val NEW_NOVEL = "novel_edit/-1"
    }
    
    object ChapterEdit : Screen("chapter_edit/{novelId}/{chapterId}") {
        fun createRoute(novelId: Long, chapterId: Long?) = 
            if (chapterId == null) "chapter_edit/$novelId/-1" else "chapter_edit/$novelId/$chapterId"
    }
    
    object CharacterEdit : Screen("character_edit/{novelId}/{characterId}") {
        fun createRoute(novelId: Long, characterId: Long?) = 
            if (characterId == null) "character_edit/$novelId/-1" else "character_edit/$novelId/$characterId"
    }
    
    object WorldviewEdit : Screen("worldview_edit/{novelId}/{worldviewId}") {
        fun createRoute(novelId: Long, worldviewId: Long?) = 
            if (worldviewId == null) "worldview_edit/$novelId/-1" else "worldview_edit/$novelId/$worldviewId"
    }
    
    object NoteEdit : Screen("note_edit/{novelId}/{noteId}") {
        fun createRoute(novelId: Long, noteId: Long?) = 
            if (noteId == null) "note_edit/$novelId/-1" else "note_edit/$novelId/$noteId"
    }
    
    object TimelineEdit : Screen("timeline_edit/{novelId}/{eventId}") {
        fun createRoute(novelId: Long, eventId: Long?) = 
            if (eventId == null) "timeline_edit/$novelId/-1" else "timeline_edit/$novelId/$eventId"
    }
    
    object ContentHistory : Screen("content_history/{targetType}/{targetId}") {
        fun createRoute(targetType: String, targetId: Long) = "content_history/$targetType/$targetId"
    }
}
