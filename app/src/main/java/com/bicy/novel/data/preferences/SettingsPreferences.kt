package com.bicy.novel.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bicy.novel.util.CryptoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeType(val displayName: String, val backgroundColor: String, val textColor: String, val secondaryColor: String) {
    CLASSIC("经典米白", "#F5F5DC", "#333333", "#E8E8D0"),
    EYE_GREEN("护眼绿", "#E7F1D5", "#2D5A27", "#D4E4C0"),
    EYE_BLUE("护眼蓝", "#D6E3FC", "#1E3A8A", "#C4D9F8"),
    EYE_GRAY("护眼灰", "#E5E5E5", "#333333", "#D5D5D5"),
    DARK("暗色模式", "#1E1E1E", "#E0E0E0", "#2D2D2D")
}

data class AppSettings(
    val aiEnabled: Boolean = false,
    val aiApiKey: String = "",
    val aiApiEndpoint: String = "https://api.openai.com/v1",
    val aiModel: String = "gpt-4",
    val aiTemperature: Float = 0.7f,
    val aiMaxTokens: Int = 8192,
    val aiMaxToolIterations: Int = 10,
    val aiEnableThinking: Boolean = false,
    val aiSystemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val aiAgentSystemPrompt: String = DEFAULT_AGENT_SYSTEM_PROMPT,
    val editorFontSize: Int = 16,
    val editorShowLineNumbers: Boolean = true,
    val editorAutoWrap: Boolean = true,
    val editorAutoSave: Boolean = true,
    val logEnabled: Boolean = true,
    val themeType: String = ThemeType.CLASSIC.name,
    val needsEncryptionUpgrade: Boolean = false,
    // 安全锁相关
    val requireSecurityOnStartup: Boolean = true, // 启动时是否需要安全验证
    val securityLockHash: String = "", // 加密后的密码哈希
    val securityQuestion: String = "", // 安全问题
    val securityAnswerHash: String = "" // 加密后的答案哈希
) {
    // 安全锁是否已设置（有密码）
    val isSecurityLockSet: Boolean get() = securityLockHash.isNotEmpty()
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "你是一个专业的小说写作助手。"
        const val DEFAULT_AGENT_SYSTEM_PROMPT = "你是一个专业的小说写作 Agent。你可以使用工具来创建、编辑、查看和搜索小说内容。\n当用户要求你写作或修改小说时，你应该使用工具来完成实际的操作。\n\n【重要】无论是否使用工具，你都必须在最后给出文字回复！\n\n【思考格式】\n在回应前，请先用特殊字符⍔和⍕包裹你的思考过程：\n⍔这里写你的思考过程⍕\n思考完成后，必须给出实际的回复内容。"
    }
    
    fun getTheme(): ThemeType {
        return try {
            ThemeType.valueOf(themeType)
        } catch (e: Exception) {
            ThemeType.CLASSIC
        }
    }
}

class SettingsPreferences(private val context: Context) {
    
    private object Keys {
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_API_ENDPOINT = stringPreferencesKey("ai_api_endpoint")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        val AI_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        val AI_MAX_TOOL_ITERATIONS = intPreferencesKey("ai_max_tool_iterations")
        val AI_ENABLE_THINKING = booleanPreferencesKey("ai_enable_thinking")
        val AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
        val AI_AGENT_SYSTEM_PROMPT = stringPreferencesKey("ai_agent_system_prompt")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val EDITOR_SHOW_LINE_NUMBERS = booleanPreferencesKey("editor_show_line_numbers")
        val EDITOR_AUTO_WRAP = booleanPreferencesKey("editor_auto_wrap")
        val EDITOR_AUTO_SAVE = booleanPreferencesKey("editor_auto_save")
        val LOG_ENABLED = booleanPreferencesKey("log_enabled")
        val THEME_TYPE = stringPreferencesKey("theme_type")
        // 安全锁相关
        val REQUIRE_SECURITY_ON_STARTUP = booleanPreferencesKey("require_security_on_startup")
        val SECURITY_LOCK_HASH = stringPreferencesKey("security_lock_hash")
        val SECURITY_QUESTION = stringPreferencesKey("security_question")
        val SECURITY_ANSWER_HASH = stringPreferencesKey("security_answer_hash")
    }
    
    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val encryptedApiKey = preferences[Keys.AI_API_KEY] ?: ""
        val decryptedApiKey = if (encryptedApiKey.isNotEmpty()) {
            CryptoUtils.decrypt(encryptedApiKey)
        } else {
            ""
        }
        
        // 检测到未加密的 API Key，标记需要升级
        val needsUpgrade = encryptedApiKey.isNotEmpty() && !CryptoUtils.isEncrypted(encryptedApiKey)
        
        AppSettings(
            aiEnabled = preferences[Keys.AI_ENABLED] ?: false,
            aiApiKey = decryptedApiKey,
            aiApiEndpoint = preferences[Keys.AI_API_ENDPOINT] ?: "https://api.openai.com/v1",
            aiModel = preferences[Keys.AI_MODEL] ?: "gpt-4",
            aiTemperature = preferences[Keys.AI_TEMPERATURE] ?: 0.7f,
            aiMaxTokens = preferences[Keys.AI_MAX_TOKENS] ?: 8192,
            aiMaxToolIterations = preferences[Keys.AI_MAX_TOOL_ITERATIONS] ?: 10,
            aiEnableThinking = preferences[Keys.AI_ENABLE_THINKING] ?: false,
            aiSystemPrompt = preferences[Keys.AI_SYSTEM_PROMPT] ?: AppSettings.DEFAULT_SYSTEM_PROMPT,
            aiAgentSystemPrompt = preferences[Keys.AI_AGENT_SYSTEM_PROMPT] ?: AppSettings.DEFAULT_AGENT_SYSTEM_PROMPT,
            editorFontSize = preferences[Keys.EDITOR_FONT_SIZE] ?: 16,
            editorShowLineNumbers = preferences[Keys.EDITOR_SHOW_LINE_NUMBERS] ?: true,
            editorAutoWrap = preferences[Keys.EDITOR_AUTO_WRAP] ?: true,
            editorAutoSave = preferences[Keys.EDITOR_AUTO_SAVE] ?: true,
            logEnabled = preferences[Keys.LOG_ENABLED] ?: true,
            themeType = preferences[Keys.THEME_TYPE] ?: ThemeType.CLASSIC.name,
            needsEncryptionUpgrade = needsUpgrade,
            requireSecurityOnStartup = preferences[Keys.REQUIRE_SECURITY_ON_STARTUP] ?: true,
            securityLockHash = preferences[Keys.SECURITY_LOCK_HASH] ?: "",
            securityQuestion = preferences[Keys.SECURITY_QUESTION] ?: "",
            securityAnswerHash = preferences[Keys.SECURITY_ANSWER_HASH] ?: ""
        )
    }
    
    suspend fun updateSettings(newSettings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_ENABLED] = newSettings.aiEnabled
            preferences[Keys.AI_API_KEY] = if (newSettings.aiApiKey.isNotEmpty()) {
                CryptoUtils.encrypt(newSettings.aiApiKey)
            } else {
                ""
            }
            preferences[Keys.AI_API_ENDPOINT] = newSettings.aiApiEndpoint
            preferences[Keys.AI_MODEL] = newSettings.aiModel
            preferences[Keys.AI_TEMPERATURE] = newSettings.aiTemperature
            preferences[Keys.AI_MAX_TOKENS] = newSettings.aiMaxTokens
            preferences[Keys.AI_MAX_TOOL_ITERATIONS] = newSettings.aiMaxToolIterations
            preferences[Keys.AI_ENABLE_THINKING] = newSettings.aiEnableThinking
            preferences[Keys.AI_SYSTEM_PROMPT] = newSettings.aiSystemPrompt
            preferences[Keys.AI_AGENT_SYSTEM_PROMPT] = newSettings.aiAgentSystemPrompt
            preferences[Keys.EDITOR_FONT_SIZE] = newSettings.editorFontSize
            preferences[Keys.EDITOR_SHOW_LINE_NUMBERS] = newSettings.editorShowLineNumbers
            preferences[Keys.EDITOR_AUTO_WRAP] = newSettings.editorAutoWrap
            preferences[Keys.EDITOR_AUTO_SAVE] = newSettings.editorAutoSave
            preferences[Keys.LOG_ENABLED] = newSettings.logEnabled
            preferences[Keys.THEME_TYPE] = newSettings.themeType
            preferences[Keys.REQUIRE_SECURITY_ON_STARTUP] = newSettings.requireSecurityOnStartup
            preferences[Keys.SECURITY_LOCK_HASH] = newSettings.securityLockHash
            preferences[Keys.SECURITY_QUESTION] = newSettings.securityQuestion
            preferences[Keys.SECURITY_ANSWER_HASH] = newSettings.securityAnswerHash
        }
    }
    
    suspend fun updateAIEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_ENABLED] = enabled
        }
    }
    
    suspend fun updateAIApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_API_KEY] = if (key.isNotEmpty()) {
                CryptoUtils.encrypt(key)
            } else {
                ""
            }
        }
    }
    
    suspend fun updateAIApiEndpoint(endpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_API_ENDPOINT] = endpoint
        }
    }
    
    suspend fun updateAIModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_MODEL] = model
        }
    }
    
    suspend fun updateAITemperature(temperature: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_TEMPERATURE] = temperature
        }
    }
    
    suspend fun updateAIMaxTokens(maxTokens: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_MAX_TOKENS] = maxTokens.coerceIn(1024, 32768)
        }
    }
    
    suspend fun updateAIMaxToolIterations(maxIterations: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_MAX_TOOL_ITERATIONS] = maxIterations.coerceIn(1, 100)
        }
    }
    
    suspend fun updateAIEnableThinking(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_ENABLE_THINKING] = enable
        }
    }
    
    suspend fun updateAISystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_SYSTEM_PROMPT] = prompt
        }
    }
    
    suspend fun updateAIAgentSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_AGENT_SYSTEM_PROMPT] = prompt
        }
    }
    
    suspend fun updateEditorFontSize(fontSize: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.EDITOR_FONT_SIZE] = fontSize
        }
    }
    
    suspend fun updateEditorShowLineNumbers(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.EDITOR_SHOW_LINE_NUMBERS] = show
        }
    }
    
    suspend fun updateEditorAutoWrap(autoWrap: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.EDITOR_AUTO_WRAP] = autoWrap
        }
    }
    
    suspend fun updateEditorAutoSave(autoSave: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.EDITOR_AUTO_SAVE] = autoSave
        }
    }
    
    suspend fun updateThemeType(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_TYPE] = theme
        }
    }
    
    suspend fun updateLogEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LOG_ENABLED] = enabled
        }
    }
    
    // 安全锁相关
    suspend fun updateRequireSecurityOnStartup(require: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.REQUIRE_SECURITY_ON_STARTUP] = require
        }
    }
    
    suspend fun updateSecurityLockHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SECURITY_LOCK_HASH] = hash
        }
    }
    
    suspend fun updateSecurityQuestion(question: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SECURITY_QUESTION] = question
        }
    }
    
    suspend fun updateSecurityAnswerHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SECURITY_ANSWER_HASH] = hash
        }
    }
    
    suspend fun updateSecurityLock(
        lockHash: String,
        question: String,
        answerHash: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SECURITY_LOCK_HASH] = lockHash
            preferences[Keys.SECURITY_QUESTION] = question
            preferences[Keys.SECURITY_ANSWER_HASH] = answerHash
        }
    }
}
