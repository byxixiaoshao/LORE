package com.bicy.novel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bicy.novel.data.preferences.AppSettings
import com.bicy.novel.data.preferences.SettingsPreferences
import com.bicy.novel.domain.repository.ContentHistoryRepository
import com.bicy.novel.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences,
    private val appLogger: AppLogger,
    private val contentHistoryRepository: ContentHistoryRepository
) : ViewModel() {
    
    private val _settings = MutableStateFlow(AppSettings())
    val settings = _settings.asStateFlow()
    
    init {
        viewModelScope.launch {
            settingsPreferences.settings.collect { appSettings ->
                _settings.value = appSettings
                
                // 自动升级未加密的 API Key
                if (appSettings.needsEncryptionUpgrade && appSettings.aiApiKey.isNotEmpty()) {
                    settingsPreferences.updateAIApiKey(appSettings.aiApiKey)
                }
            }
        }
    }
    
    fun updateAIEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.updateAIEnabled(enabled)
            _settings.update { it.copy(aiEnabled = enabled) }
        }
    }
    
    fun updateAIApiKey(key: String) {
        viewModelScope.launch {
            settingsPreferences.updateAIApiKey(key)
            _settings.update { it.copy(aiApiKey = key) }
        }
    }
    
    fun updateAIApiEndpoint(endpoint: String) {
        viewModelScope.launch {
            settingsPreferences.updateAIApiEndpoint(endpoint)
            _settings.update { it.copy(aiApiEndpoint = endpoint) }
        }
    }
    
    fun updateAIModel(model: String) {
        viewModelScope.launch {
            settingsPreferences.updateAIModel(model)
            _settings.update { it.copy(aiModel = model) }
        }
    }
    
    fun updateAITemperature(temperature: Float) {
        viewModelScope.launch {
            settingsPreferences.updateAITemperature(temperature)
            _settings.update { it.copy(aiTemperature = temperature) }
        }
    }
    
    fun updateAIMaxTokens(maxTokens: Int) {
        viewModelScope.launch {
            val clamped = maxTokens.coerceIn(1024, 32768)
            settingsPreferences.updateAIMaxTokens(clamped)
            _settings.update { it.copy(aiMaxTokens = clamped) }
        }
    }
    
    fun updateAIMaxToolIterations(maxIterations: Int) {
        viewModelScope.launch {
            val clamped = maxIterations.coerceIn(1, 100)
            settingsPreferences.updateAIMaxToolIterations(clamped)
            _settings.update { it.copy(aiMaxToolIterations = clamped) }
        }
    }
    
    fun updateAIEnableThinking(enable: Boolean) {
        viewModelScope.launch {
            settingsPreferences.updateAIEnableThinking(enable)
            _settings.update { it.copy(aiEnableThinking = enable) }
        }
    }
    
    fun updateAISystemPrompt(prompt: String) {
        viewModelScope.launch {
            settingsPreferences.updateAISystemPrompt(prompt)
            _settings.update { it.copy(aiSystemPrompt = prompt) }
        }
    }
    
    fun updateAIAgentSystemPrompt(prompt: String) {
        viewModelScope.launch {
            settingsPreferences.updateAIAgentSystemPrompt(prompt)
            _settings.update { it.copy(aiAgentSystemPrompt = prompt) }
        }
    }
    
    fun resetSystemPrompt() {
        viewModelScope.launch {
            settingsPreferences.updateAISystemPrompt(AppSettings.DEFAULT_SYSTEM_PROMPT)
            _settings.update { it.copy(aiSystemPrompt = AppSettings.DEFAULT_SYSTEM_PROMPT) }
        }
    }
    
    fun resetAgentSystemPrompt() {
        viewModelScope.launch {
            settingsPreferences.updateAIAgentSystemPrompt(AppSettings.DEFAULT_AGENT_SYSTEM_PROMPT)
            _settings.update { it.copy(aiAgentSystemPrompt = AppSettings.DEFAULT_AGENT_SYSTEM_PROMPT) }
        }
    }
    
    fun updateEditorFontSize(fontSize: Int) {
        viewModelScope.launch {
            settingsPreferences.updateEditorFontSize(fontSize)
            _settings.update { it.copy(editorFontSize = fontSize) }
        }
    }
    
    fun updateEditorShowLineNumbers(show: Boolean) {
        viewModelScope.launch {
            settingsPreferences.updateEditorShowLineNumbers(show)
            _settings.update { it.copy(editorShowLineNumbers = show) }
        }
    }
    
    fun updateEditorAutoWrap(autoWrap: Boolean) {
        viewModelScope.launch {
            settingsPreferences.updateEditorAutoWrap(autoWrap)
            _settings.update { it.copy(editorAutoWrap = autoWrap) }
        }
    }
    
    fun updateEditorAutoSave(autoSave: Boolean) {
        viewModelScope.launch {
            settingsPreferences.updateEditorAutoSave(autoSave)
            _settings.update { it.copy(editorAutoSave = autoSave) }
        }
    }
    
    fun updateThemeType(theme: String) {
        viewModelScope.launch {
            settingsPreferences.updateThemeType(theme)
            _settings.update { it.copy(themeType = theme) }
        }
    }
    
    fun updateAllSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsPreferences.updateSettings(newSettings)
            _settings.value = newSettings
        }
    }
    
    fun updateLogEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsPreferences.updateLogEnabled(enabled)
            if (enabled) {
                appLogger.startLogcatCapture()
            } else {
                appLogger.stopLogcatCapture()
            }
            _settings.update { it.copy(logEnabled = enabled) }
        }
    }
    
    private val _appLogContent = MutableStateFlow("")
    val appLogContent = _appLogContent.asStateFlow()
    
    private val _crashLogContent = MutableStateFlow("")
    val crashLogContent = _crashLogContent.asStateFlow()
    
    private val _logFiles = MutableStateFlow<List<File>>(emptyList())
    val logFiles = _logFiles.asStateFlow()
    
    private val _selectedLogContent = MutableStateFlow("")
    val selectedLogContent = _selectedLogContent.asStateFlow()
    
    private val _selectedLogTitle = MutableStateFlow("")
    val selectedLogTitle = _selectedLogTitle.asStateFlow()
    
    fun loadAppLog() {
        viewModelScope.launch {
            _appLogContent.value = appLogger.getAppLogContent()
        }
    }
    
    fun loadCrashLog() {
        viewModelScope.launch {
            _crashLogContent.value = appLogger.getCrashLogContent()
        }
    }
    
    fun loadLogFiles() {
        viewModelScope.launch {
            _logFiles.value = appLogger.getAllLogFiles()
        }
    }
    
    fun loadLogFile(file: File) {
        viewModelScope.launch {
            _selectedLogTitle.value = file.name
            _selectedLogContent.value = appLogger.readLogFile(file)
        }
    }
    
    fun clearAppLog() {
        viewModelScope.launch {
            appLogger.clearAppLog()
            _appLogContent.value = ""
        }
    }
    
    fun clearCrashLog() {
        viewModelScope.launch {
            appLogger.clearCrashLog()
            _crashLogContent.value = ""
        }
    }
    
    fun clearAllLogs() {
        viewModelScope.launch {
            appLogger.clearAllLogs()
            _appLogContent.value = ""
            _crashLogContent.value = ""
            _logFiles.value = emptyList()
        }
    }
    
    private val _dbDiagnostic = MutableStateFlow<String?>(null)
    val dbDiagnostic = _dbDiagnostic.asStateFlow()
    
    fun runDbDiagnose() {
        viewModelScope.launch {
            try {
                _dbDiagnostic.value = "正在诊断..."
                _dbDiagnostic.value = contentHistoryRepository.diagnoseTableSize()
            } catch (e: Exception) {
                _dbDiagnostic.value = "诊断失败: ${e.message}"
            }
        }
    }
    
    fun runVacuum() {
        viewModelScope.launch {
            try {
                contentHistoryRepository.vacuum()
                _dbDiagnostic.value = "VACUUM 完成\n\n" + contentHistoryRepository.diagnoseTableSize()
            } catch (e: Exception) {
                _dbDiagnostic.value = "VACUUM 失败: ${e.message}"
            }
        }
    }
    
    private val _logFileSizeMb = MutableStateFlow(0.0)
    val logFileSizeMb = _logFileSizeMb.asStateFlow()
    
    fun loadLogFileSize() {
        viewModelScope.launch {
            _logFileSizeMb.value = appLogger.getTotalLogSizeMb()
        }
    }
    
    // 安全锁相关
    fun updateRequireSecurityOnStartup(require: Boolean) {
        viewModelScope.launch {
            settingsPreferences.updateRequireSecurityOnStartup(require)
            _settings.update { it.copy(requireSecurityOnStartup = require) }
        }
    }
    
    fun setSecurityLock(password: String, question: String, answer: String) {
        viewModelScope.launch {
            val lockEncrypted = com.bicy.novel.ui.components.SecurityLockUtils.encryptPassword(password)
            val answerEncrypted = com.bicy.novel.ui.components.SecurityLockUtils.encryptPassword(answer)
            settingsPreferences.updateSecurityLock(lockEncrypted, question, answerEncrypted)
            _settings.update { 
                it.copy(
                    securityLockHash = lockEncrypted,
                    securityQuestion = question,
                    securityAnswerHash = answerEncrypted
                )
            }
        }
    }
    
    fun verifySecurityLockPassword(password: String): Boolean {
        return com.bicy.novel.ui.components.SecurityLockUtils.verifyPassword(
            password, 
            _settings.value.securityLockHash
        )
    }
    
    fun verifySecurityAnswer(answer: String): Boolean {
        return com.bicy.novel.ui.components.SecurityLockUtils.verifyPassword(
            answer,
            _settings.value.securityAnswerHash
        )
    }
    
    fun updateSecurityQuestion(question: String, answer: String) {
        viewModelScope.launch {
            val answerEncrypted = com.bicy.novel.ui.components.SecurityLockUtils.encryptPassword(answer)
            settingsPreferences.updateSecurityQuestion(question)
            settingsPreferences.updateSecurityAnswerHash(answerEncrypted)
            _settings.update {
                it.copy(
                    securityQuestion = question,
                    securityAnswerHash = answerEncrypted
                )
            }
        }
    }
    
    fun clearSecurityLock() {
        viewModelScope.launch {
            settingsPreferences.updateSecurityLock("", "", "")
            _settings.update {
                it.copy(
                    securityLockHash = "",
                    securityQuestion = "",
                    securityAnswerHash = ""
                )
            }
        }
    }
}
