package com.bicy.novel.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bicy.novel.data.export.ExportResult
import com.bicy.novel.data.export.ImportResult
import com.bicy.novel.ui.MainActivity
import com.bicy.novel.ui.components.ExportOptions
import com.bicy.novel.ui.components.ExportOptionsDialog
import com.bicy.novel.ui.components.ImportPasswordDialog
import com.bicy.novel.ui.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController,
    activity: MainActivity,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.NovelList.route,
    exportImportViewModel: ExportImportViewModel = hiltViewModel()
) {
    val exportState by exportImportViewModel.exportState.collectAsState()
    val importState by exportImportViewModel.importState.collectAsState()
    
    var pendingExportOptions by remember { mutableStateOf<ExportOptions?>(null) }
    var currentNovelTitle by remember { mutableStateOf("") }
    var showExportSuccess by remember { mutableStateOf(false) }
    var showImportSuccess by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(exportState.result) {
        when (val result = exportState.result) {
            is ExportResult.Success -> {
                showExportSuccess = true
                exportImportViewModel.clearExportResult()
            }
            is ExportResult.Error -> {
                showErrorMessage = result.message
                exportImportViewModel.clearExportResult()
            }
            null -> {}
        }
    }
    
    LaunchedEffect(importState.result) {
        when (val result = importState.result) {
            is ImportResult.Success -> {
                showImportSuccess = true
                navController.navigate(Screen.NovelDetail.createRoute(result.novelId))
                exportImportViewModel.clearImportResult()
            }
            is ImportResult.Error -> {
                showErrorMessage = result.message
                exportImportViewModel.clearImportResult()
            }
            is ImportResult.NeedPassword -> {}
            null -> {}
        }
    }
    
    if (exportState.showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { exportImportViewModel.hideExportDialog() },
            onConfirm = { options ->
                pendingExportOptions = options
                val fileName = if (options.isBackup) {
                    "${currentNovelTitle}_backup.zip"
                } else {
                    "${currentNovelTitle}.zip"
                }
                activity.startExport(fileName) { uri ->
                    exportImportViewModel.exportProject(uri, options)
                }
            },
            isExporting = exportState.isExporting
        )
    }
    
    if (importState.showPasswordDialog) {
        ImportPasswordDialog(
            onDismiss = { exportImportViewModel.hidePasswordDialog() },
            onConfirm = { password ->
                exportImportViewModel.importWithPassword(password)
            },
            isImporting = importState.isImporting
        )
    }
    
    if (showExportSuccess) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExportSuccess = false },
            title = { androidx.compose.material3.Text("导出成功") },
            text = { androidx.compose.material3.Text("项目已成功导出") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showExportSuccess = false }) {
                    androidx.compose.material3.Text("确定")
                }
            }
        )
    }
    
    if (showImportSuccess) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImportSuccess = false },
            title = { androidx.compose.material3.Text("导入成功") },
            text = { androidx.compose.material3.Text("项目已成功导入") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showImportSuccess = false }) {
                    androidx.compose.material3.Text("确定")
                }
            }
        )
    }
    
    showErrorMessage?.let { message ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showErrorMessage = null },
            title = { androidx.compose.material3.Text("错误") },
            text = { androidx.compose.material3.Text(message) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showErrorMessage = null }) {
                    androidx.compose.material3.Text("确定")
                }
            }
        )
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.NovelList.route) {
            NovelListScreen(
                onNovelClick = { novelId ->
                    navController.navigate(Screen.NovelDetail.createRoute(novelId))
                },
                onAddNovelClick = {
                    navController.navigate(Screen.NovelEdit.NEW_NOVEL)
                },
                onImportNovelClick = {
                    activity.startImport { uri ->
                        exportImportViewModel.startImport(uri)
                    }
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToLogViewer = {
                    navController.navigate(Screen.LogViewer.route)
                }
            )
        }
        
        composable(Screen.LogViewer.route) {
            LogViewerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.NovelDetail.route,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            NovelDetailScreen(
                novelId = novelId,
                onBackClick = { navController.popBackStack() },
                onEditNovelClick = { navController.navigate(Screen.NovelEdit.createRoute(novelId)) },
                onEditChapterClick = { chapterId ->
                    navController.navigate(Screen.ChapterEdit.createRoute(novelId, chapterId))
                },
                onEditCharacterClick = { characterId ->
                    navController.navigate(Screen.CharacterEdit.createRoute(novelId, characterId))
                },
                onEditWorldviewClick = { worldviewId ->
                    navController.navigate(Screen.WorldviewEdit.createRoute(novelId, worldviewId))
                },
                onEditNoteClick = { noteId ->
                    navController.navigate(Screen.NoteEdit.createRoute(novelId, noteId))
                },
                onEditTimelineClick = { eventId ->
                    navController.navigate(Screen.TimelineEdit.createRoute(novelId, eventId))
                },
                onExportClick = {
                    currentNovelTitle = "novel_$novelId"
                    exportImportViewModel.showExportDialog(novelId)
                }
            )
        }
        
        composable(
            route = Screen.NovelEdit.route,
            arguments = listOf(navArgument("novelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: -1L
            NovelEditScreen(
                novelId = if (novelId == -1L) null else novelId,
                onBackClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ChapterEdit.route,
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType },
                navArgument("chapterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: -1L
            
            ChapterEditScreen(
                novelId = novelId,
                chapterId = if (chapterId == -1L) null else chapterId,
                onBackClick = { navController.popBackStack() },
                onNavigateToChapter = { newChapterId ->
                    navController.navigate(Screen.ChapterEdit.createRoute(novelId, newChapterId))
                },
                onNavigateToCharacter = { characterId ->
                    navController.navigate(Screen.CharacterEdit.createRoute(novelId, characterId))
                },
                onNavigateToWorldview = { worldviewId ->
                    navController.navigate(Screen.WorldviewEdit.createRoute(novelId, worldviewId))
                },
                onNavigateToNote = { noteId ->
                    navController.navigate(Screen.NoteEdit.createRoute(novelId, noteId))
                },
                onNavigateToTimeline = { eventId ->
                    navController.navigate(Screen.TimelineEdit.createRoute(novelId, eventId))
                }
            )
        }
        
        composable(
            route = Screen.CharacterEdit.route,
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType },
                navArgument("characterId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            val characterId = backStackEntry.arguments?.getLong("characterId") ?: -1L
            CharacterEditScreen(
                novelId = novelId,
                characterId = if (characterId == -1L) null else characterId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.WorldviewEdit.route,
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType },
                navArgument("worldviewId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            val worldviewId = backStackEntry.arguments?.getLong("worldviewId") ?: -1L
            WorldviewEditScreen(
                novelId = novelId,
                worldviewId = if (worldviewId == -1L) null else worldviewId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.NoteEdit.route,
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType },
                navArgument("noteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            NoteEditScreen(
                novelId = novelId,
                noteId = if (noteId == -1L) null else noteId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.TimelineEdit.route,
            arguments = listOf(
                navArgument("novelId") { type = NavType.LongType },
                navArgument("eventId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getLong("novelId") ?: 0L
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L
            TimelineEditScreen(
                novelId = novelId,
                eventId = if (eventId == -1L) null else eventId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
