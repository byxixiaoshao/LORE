package com.bicy.novel.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log as AndroidLog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bicy.novel.data.preferences.AppSettings
import com.bicy.novel.data.preferences.SettingsPreferences
import com.bicy.novel.domain.repository.NovelRepository
import com.bicy.novel.ui.navigation.AppNavigation
import com.bicy.novel.ui.navigation.Screen
import com.bicy.novel.ui.screens.SplashScreen
import com.bicy.novel.ui.theme.NovelEditorTheme
import com.bicy.novel.util.AppLogger
import com.bicy.novel.domain.repository.ContentHistoryRepository
import com.bicy.novel.ui.components.SecurityLockInputDialog
import com.bicy.novel.ui.components.SecurityQuestionDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsPreferences: SettingsPreferences
    
    @Inject
    lateinit var appLogger: AppLogger
    
    @Inject
    lateinit var contentHistoryRepository: ContentHistoryRepository
    
    @Inject
    lateinit var novelRepository: NovelRepository
    
    private var exportCallback: ((Uri) -> Unit)? = null
    private var importCallback: ((Uri) -> Unit)? = null
    
    // 预加载状态
    private var isPreloaded = false
    private val preloadCallback = mutableListOf<() -> Unit>()
    
    fun onPreloadComplete(callback: () -> Unit) {
        if (isPreloaded) {
            callback()
        } else {
            preloadCallback.add(callback)
        }
    }
    
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { exportCallback?.invoke(it) }
    }
    
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importCallback?.invoke(it) }
    }
    
    fun startExport(fileName: String, callback: (Uri) -> Unit) {
        exportCallback = callback
        exportLauncher.launch(fileName)
    }
    
    fun startImport(callback: (Uri) -> Unit) {
        importCallback = callback
        importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
    }
    
    private val cleanupScope = CoroutineScope(Dispatchers.IO)
    private val preloadScope = CoroutineScope(Dispatchers.IO)
    
    private fun runDbCleanup() {
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        val lastCleanupVersion = prefs.getInt("db_cleanup_version", 0)
        
        // 版本号递增，确保每次升级只清理一次
        if (lastCleanupVersion < 2) {
            cleanupScope.launch {
                try {
                    // 诊断各表大小
                    val diag = contentHistoryRepository.diagnoseTableSize()
                    AndroidLog.i("DB_DIAG", "\n$diag")
                    
                    val deleted = contentHistoryRepository.cleanUpExcessHistory()
                    AndroidLog.i("DB_CLEANUP", "Removed $deleted excess history entries")
                    
                    contentHistoryRepository.vacuum()
                    AndroidLog.i("DB_CLEANUP", "VACUUM complete")
                    
                    prefs.edit().putInt("db_cleanup_version", 2).apply()
                } catch (e: Exception) {
                    AndroidLog.w("DB_CLEANUP", "Cleanup error: ${e.message}")
                }
            }
        }
    }
    
    // 预加载数据（在开屏动画期间执行）
    private fun preloadData() {
        if (isPreloaded) return
        
        preloadScope.launch {
            try {
                AndroidLog.i("MainActivity", "开始预加载数据...")
                
                // 预加载项目列表
                novelRepository.getAllNovels().first()
                
                // 预加载设置
                settingsPreferences.settings.first()
                
                isPreloaded = true
                AndroidLog.i("MainActivity", "预加载完成")
                
                // 通知所有等待的回调
                withContext(Dispatchers.Main) {
                    preloadCallback.forEach { it() }
                    preloadCallback.clear()
                }
            } catch (e: Exception) {
                AndroidLog.w("MainActivity", "预加载失败: ${e.message}")
                isPreloaded = true
                withContext(Dispatchers.Main) {
                    preloadCallback.forEach { it() }
                    preloadCallback.clear()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (AppSettings().logEnabled) {
            appLogger.startLogcatCapture()
        }
        
        // 首次启动时清理历史记录膨胀
        runDbCleanup()
        
        // 在开屏动画期间预加载数据
        preloadData()
        
        setContent {
            val settings by settingsPreferences.settings.collectAsState(initial = AppSettings())
            val themeType = remember(settings.themeType) { settings.getTheme() }
            
            // 开屏动画状态
            var showSplash by remember { mutableStateOf(true) }
            
            // 数据加载状态
            var isDataLoaded by remember { mutableStateOf(false) }
            
            // 安全锁状态
            var isUnlocked by remember { mutableStateOf(false) }
            var showSetupDialog by remember { mutableStateOf(false) }
            var showVerifyDialog by remember { mutableStateOf(false) }
            var showQuestionDialog by remember { mutableStateOf(false) }
            var tempPassword by remember { mutableStateOf("") }
            
            LaunchedEffect(settings.logEnabled) {
                if (settings.logEnabled) {
                    appLogger.startLogcatCapture()
                } else {
                    appLogger.stopLogcatCapture()
                }
            }
            
            // 监听预加载完成
            LaunchedEffect(Unit) {
                onPreloadComplete {
                    isDataLoaded = true
                }
            }
            
            // 检查安全锁状态（在开屏动画期间预检查）
            LaunchedEffect(settings.isSecurityLockSet, settings.requireSecurityOnStartup, showSplash) {
                if (!showSplash) {
                    if (settings.isSecurityLockSet && settings.requireSecurityOnStartup && !isUnlocked) {
                        showVerifyDialog = true
                    } else if (!settings.isSecurityLockSet && !isUnlocked) {
                        // 未设置安全锁，强制设置
                        showSetupDialog = true
                    } else if (!settings.requireSecurityOnStartup || !settings.isSecurityLockSet) {
                        // 不需要启动验证或未设置安全锁，直接解锁
                        isUnlocked = true
                    }
                }
            }
            
            NovelEditorTheme(themeType = themeType) {
                // 开屏动画
                if (showSplash) {
                    SplashScreen(
                        isDataLoaded = isDataLoaded,
                        onAnimationComplete = {
                            showSplash = false
                            // 如果不需要启动验证，直接解锁
                            if (!settings.requireSecurityOnStartup || !settings.isSecurityLockSet) {
                                isUnlocked = true
                            }
                        }
                    )
                } else {
                    // 安全锁设置对话框（首次设置）
                    if (showSetupDialog && !settings.isSecurityLockSet) {
                        SecurityLockInputDialog(
                            title = "设置安全锁",
                            isRegisterMode = true,
                            showCancelButton = false,
                            onConfirm = { password ->
                                tempPassword = password
                                showSetupDialog = false
                                showQuestionDialog = true
                            },
                            onDismiss = { }
                        )
                    }
                    
                    // 安全问题设置对话框
                    if (showQuestionDialog && !settings.isSecurityLockSet) {
                        SecurityQuestionDialog(
                            isSetupMode = true,
                            onConfirm = { question, answer ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val lockEncrypted = com.bicy.novel.ui.components.SecurityLockUtils.encryptPassword(tempPassword)
                                    val answerEncrypted = com.bicy.novel.ui.components.SecurityLockUtils.encryptPassword(answer)
                                    settingsPreferences.updateSecurityLock(lockEncrypted, question, answerEncrypted)
                                }
                                showQuestionDialog = false
                                tempPassword = ""
                                isUnlocked = true
                            },
                            onDismiss = {
                                showQuestionDialog = false
                                tempPassword = ""
                                showSetupDialog = true
                            }
                        )
                    }
                    
                    // 安全锁验证对话框
                    if (showVerifyDialog && settings.isSecurityLockSet) {
                        SecurityLockInputDialog(
                            title = "安全锁",
                            subtitle = "请输入密码解锁",
                            isRegisterMode = false,
                            showCancelButton = false,
                            onConfirm = { password ->
                                if (com.bicy.novel.ui.components.SecurityLockUtils.verifyPassword(
                                        password,
                                        settings.securityLockHash
                                    )
                                ) {
                                    showVerifyDialog = false
                                    isUnlocked = true
                                }
                            },
                            onDismiss = { },
                            onForgotPassword = {
                                showVerifyDialog = false
                                showQuestionDialog = true
                            }
                        )
                    }
                    
                    // 安全问题验证对话框（忘记密码）
                    if (showQuestionDialog && settings.isSecurityLockSet) {
                        SecurityQuestionDialog(
                            isSetupMode = false,
                            existingQuestion = settings.securityQuestion,
                            onConfirm = { _, _ ->
                                showQuestionDialog = false
                                isUnlocked = true
                            },
                            onDismiss = {
                                showQuestionDialog = false
                                showVerifyDialog = true
                            },
                            onVerifyAnswer = { answer ->
                                com.bicy.novel.ui.components.SecurityLockUtils.verifyPassword(
                                    answer,
                                    settings.securityAnswerHash
                                )
                            }
                        )
                    }
                    
                    // 主界面（解锁后显示）
                    if (isUnlocked || !settings.isSecurityLockSet || !settings.requireSecurityOnStartup) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            val navController = rememberNavController()
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            
                            val showBottomBar = currentRoute in listOf(Screen.NovelList.route, Screen.Settings.route)
                            
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                contentWindowInsets = WindowInsets(0.dp),
                                bottomBar = {
                                    if (showBottomBar) {
                                        NavigationBar {
                                            NavigationBarItem(
                                                selected = currentRoute == Screen.NovelList.route,
                                                onClick = {
                                                    if (currentRoute != Screen.NovelList.route) {
                                                        navController.navigate(Screen.NovelList.route) {
                                                            popUpTo(Screen.NovelList.route) { inclusive = true }
                                                        }
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.Book, contentDescription = null) },
                                                label = { Text("项目") }
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == Screen.Settings.route,
                                                onClick = {
                                                    if (currentRoute != Screen.Settings.route) {
                                                        navController.navigate(Screen.Settings.route) {
                                                            launchSingleTop = true
                                                        }
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                                label = { Text("设置") }
                                            )
                                        }
                                    }
                                }
                            ) { paddingValues ->
                                AppNavigation(
                                    navController = navController,
                                    activity = this@MainActivity,
                                    modifier = Modifier.padding(paddingValues)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
