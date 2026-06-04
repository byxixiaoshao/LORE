package com.bicy.novel.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenAIProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val maxRetries: Int = 5,
    private val connectTimeout: Int = 30000,
    private val readTimeout: Int = 120000
) : AIProvider {
    
    companion object {
        private const val TAG = "OpenAIProvider"
    }
    
    override suspend fun chat(request: ChatRequest): Result<ChatResponse> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        for (attempt in 1..maxRetries) {
            if (attempt > 1) {
                val delayMs = when (attempt) {
                    2 -> 2000L
                    3 -> 3000L
                    4 -> 5000L
                    else -> 8000L
                }
                Log.d(TAG, "等待 ${delayMs}ms 后重试 (第${attempt}次)...")
                delay(delayMs)
            }
            
            Log.d(TAG, "请求尝试 $attempt/$maxRetries")
            
            val result = executeChatRequest(request)
            
            if (result.isSuccess) {
                if (attempt > 1) {
                    Log.d(TAG, "重试成功 (第${attempt}次)")
                }
                return@withContext result
            }
            
            val error = result.exceptionOrNull() as? Exception ?: Exception("未知错误")
            lastException = error
            
            if (!isRetryableError(error)) {
                Log.d(TAG, "不可重试的错误: ${error.message}")
                return@withContext result
            }
            
            Log.w(TAG, "可重试错误 (第${attempt}次): ${error.message}")
        }
        
        Log.e(TAG, "所有重试失败 ($maxRetries 次)")
        Result.failure(Exception("请求失败(已重试 $maxRetries 次): ${lastException?.message}"))
    }
    
    private suspend fun executeChatRequest(request: ChatRequest): Result<ChatResponse> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        return@withContext try {
            val fullUrl = "$baseUrl/chat/completions"
            Log.d(TAG, "请求 URL: $fullUrl")
            Log.d(TAG, "请求模型: ${request.model}")
            
            val url = URL(fullUrl)
            connection = url.openConnection() as HttpURLConnection
            
            connection!!.requestMethod = "POST"
            connection!!.setRequestProperty("Authorization", "Bearer $apiKey")
            connection!!.setRequestProperty("Content-Type", "application/json")
            connection!!.doOutput = true
            connection!!.connectTimeout = connectTimeout
            connection!!.readTimeout = readTimeout
            
            val requestBody = buildRequestBody(request)
            Log.d(TAG, "请求体大小: ${requestBody.length} 字符")
            
            connection!!.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
            }
            
            val responseCode = connection!!.responseCode
            Log.d(TAG, "响应码: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection!!.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "响应体大小: ${responseBody.length} 字符")
                parseResponse(responseBody)
            } else {
                val errorBody = connection!!.errorStream?.bufferedReader()?.use { it.readText() } ?: "无错误详情"
                Log.e(TAG, "错误响应: $errorBody")
                val errorMessage = parseErrorMessage(errorBody) ?: "HTTP $responseCode"
                Result.failure(Exception("API 错误 ($responseCode): $errorMessage"))
            }
        } catch (e: java.net.MalformedURLException) {
            Log.e(TAG, "URL格式错误", e)
            Result.failure(Exception("URL 格式错误：请检查 API 端点格式是否正确 (如: https://api.openai.com/v1)"))
        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "请求超时 (connectTimeout=${connectTimeout}ms, readTimeout=${readTimeout}ms)")
            Result.failure(Exception("请求超时"))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "无法解析主机名: $baseUrl", e)
            Result.failure(Exception("无法解析主机名：请检查 API 端点\n当前端点: $baseUrl"))
        } catch (e: java.net.ConnectException) {
            Log.w(TAG, "连接被拒绝", e)
            Result.failure(Exception("连接被拒绝"))
        } catch (e: java.io.IOException) {
            Log.w(TAG, "IO错误: ${e.message}")
            Result.failure(Exception("网络 IO 错误: ${e.message ?: "未知"}"))
        } catch (e: Exception) {
            Log.e(TAG, "未知异常: ${e.javaClass.name}", e)
            val errorDetail = if (e.message != null) "${e.javaClass.simpleName}: ${e.message}" else e.javaClass.simpleName
            Result.failure(Exception("请求异常: $errorDetail"))
        } finally {
            connection?.disconnect()
        }
    }
    
    private fun isRetryableError(error: Exception): Boolean {
        val message = error.message ?: return false
        return message.contains("超时") ||
               message.contains("IO 错误") ||
               message.contains("连接被拒绝") ||
               message.contains("SocketTimeoutException") ||
               message.contains("无法解析主机名") ||
               message.contains("UnknownHostException")
    }
    
    private fun parseErrorMessage(errorBody: String): String? {
        return try {
            val json = JSONObject(errorBody)
            val errorObj = json.optJSONObject("error")
            errorObj?.optString("message")
        } catch (e: Exception) {
            null
        }
    }
    
    override fun chatStream(request: ChatRequest): Flow<Result<StreamChunk>> = flow {
        var retryCount = 0
        var lastError: Exception? = null
        
        while (retryCount < maxRetries) {
            if (retryCount > 0) {
                val delayMs = retryCount * 1000L
                Log.d(TAG, "流式请求等待 ${delayMs}ms 后重试 (第${retryCount + 1}次)...")
                delay(delayMs)
            }
            
            try {
                val fullUrl = "$baseUrl/chat/completions"
                Log.d(TAG, "流式请求 URL: $fullUrl")
                
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = connectTimeout
                connection.readTimeout = readTimeout * 2
                
                val requestBody = buildRequestBody(request, stream = true)
                Log.d(TAG, "流式请求体大小: ${requestBody.length} 字符")
                
                connection.outputStream.use { os ->
                    os.write(requestBody.toByteArray(Charsets.UTF_8))
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "流式响应码: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    var success = false
                    connection.inputStream.bufferedReader().use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            if (line.startsWith("data: ")) {
                                val data = line.substring(6)
                                if (data != "[DONE]") {
                                    try {
                                        val chunk = parseStreamChunk(data)
                                        if (chunk != null) {
                                            emit(Result.success(chunk))
                                            success = true
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "解析流式块失败: $data")
                                    }
                                }
                            }
                            line = reader.readLine()
                        }
                    }
                    if (success) return@flow
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "无错误详情"
                    Log.e(TAG, "流式错误响应: $errorBody")
                    val errorMessage = parseErrorMessage(errorBody) ?: "HTTP $responseCode"
                    lastError = Exception("API 错误 ($responseCode): $errorMessage")
                    if (!errorMessage.contains("rate limit") && responseCode != 401 && responseCode != 403) {
                        retryCount++
                        continue
                    }
                    emit(Result.failure(lastError!!))
                    return@flow
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.w(TAG, "流式请求超时")
                lastError = e
                retryCount++
                continue
            } catch (e: java.io.IOException) {
                Log.w(TAG, "流式IO错误: ${e.message}")
                lastError = e
                retryCount++
                continue
            } catch (e: Exception) {
                Log.e(TAG, "流式请求异常", e)
                emit(Result.failure(Exception("流式请求失败: ${e.message}")))
                return@flow
            }
            
            break
        }
        
        if (retryCount >= maxRetries) {
            emit(Result.failure(Exception("流式请求失败(已重试 $maxRetries 次): ${lastError?.message}")))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun testConnection(): Result<Boolean> {
        return try {
            val testRequest = ChatRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(ChatMessage("user", "Hello")),
                maxTokens = 10
            )
            val result = chat(testRequest)
            Result.success(result.isSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildRequestBody(request: ChatRequest, stream: Boolean = false): String {
        val messagesArray = JSONArray()
        request.messages.forEach { msg ->
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)
            
            if (msg.role == "tool") {
                msgObj.put("content", msg.content)
                msgObj.put("tool_call_id", msg.toolCallId)
            } else {
                msgObj.put("content", msg.content)
                if (msg.toolCalls != null && msg.toolCalls.isNotEmpty()) {
                    val toolCallsArray = JSONArray()
                    msg.toolCalls.forEach { tc ->
                        val tcObj = JSONObject()
                        tcObj.put("id", tc.id)
                        tcObj.put("type", tc.type)
                        val funcObj = JSONObject()
                        funcObj.put("name", tc.function.name)
                        funcObj.put("arguments", tc.function.arguments)
                        tcObj.put("function", funcObj)
                        toolCallsArray.put(tcObj)
                    }
                    msgObj.put("tool_calls", toolCallsArray)
                }
            }
            messagesArray.put(msgObj)
        }
        
        val body = JSONObject()
        body.put("model", request.model)
        body.put("messages", messagesArray)
        body.put("temperature", request.temperature)
        body.put("max_tokens", request.maxTokens)
        body.put("stream", stream)
        
        if (request.tools != null && request.tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            request.tools.forEach { tool ->
                toolsArray.put(JSONObject(tool))
            }
            body.put("tools", toolsArray)
        }
        
        if (request.enableThinking) {
            val modelLower = request.model.lowercase()
            if (modelLower.contains("deepseek") || modelLower.contains("r1")) {
                body.put("reasoning_effort", "medium")
            }
        }
        
        return body.toString()
    }
    
    private fun parseResponse(responseBody: String): Result<ChatResponse> {
        return try {
            val json = JSONObject(responseBody)
            val id = json.getString("id")
            val choicesArray = json.getJSONArray("choices")
            
            val choices = mutableListOf<ChatChoice>()
            for (i in 0 until choicesArray.length()) {
                val choiceObj = choicesArray.getJSONObject(i)
                val messageObj = choiceObj.getJSONObject("message")
                
                val toolCalls = if (messageObj.has("tool_calls")) {
                    val tcArray = messageObj.getJSONArray("tool_calls")
                    val calls = mutableListOf<ToolCall>()
                    for (j in 0 until tcArray.length()) {
                        val tcObj = tcArray.getJSONObject(j)
                        val funcObj = tcObj.getJSONObject("function")
                        calls.add(ToolCall(
                            id = tcObj.getString("id"),
                            type = tcObj.optString("type", "function"),
                            function = ToolCallFunction(
                                name = funcObj.getString("name"),
                                arguments = funcObj.getString("arguments")
                            )
                        ))
                    }
                    calls
                } else null
                
                val content = messageObj.optString("content", "")
                val reasoningContent = messageObj.optString("reasoning_content", "")
                val finalContent = if (reasoningContent.isNotEmpty()) {
                    "\u2354$reasoningContent\u2355$content"
                } else {
                    content
                }
                
                choices.add(
                    ChatChoice(
                        index = choiceObj.getInt("index"),
                        message = ChatMessage(
                            role = messageObj.getString("role"),
                            content = finalContent,
                            toolCalls = toolCalls
                        ),
                        finishReason = choiceObj.optString("finish_reason")
                    )
                )
            }
            
            var usage: ChatUsage? = null
            if (json.has("usage")) {
                val usageObj = json.getJSONObject("usage")
                usage = ChatUsage(
                    promptTokens = usageObj.getInt("prompt_tokens"),
                    completionTokens = usageObj.getInt("completion_tokens"),
                    totalTokens = usageObj.getInt("total_tokens")
                )
            }
            
            Result.success(ChatResponse(id = id, choices = choices, usage = usage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseStreamChunk(data: String): StreamChunk? {
        val json = JSONObject(data)
        val choicesArray = json.getJSONArray("choices")
        if (choicesArray.length() == 0) return null
        
        val choiceObj = choicesArray.getJSONObject(0)
        if (!choiceObj.has("delta")) return null
        
        val deltaObj = choiceObj.getJSONObject("delta")
        val content = deltaObj.optString("content", "")
        val reasoningContent = deltaObj.optString("reasoning_content", "")
        val role = deltaObj.optString("role", "assistant")
        
        val finalContent = when {
            reasoningContent.isNotEmpty() && content.isNotEmpty() -> "\u2354$reasoningContent\u2355$content"
            reasoningContent.isNotEmpty() -> "\u2354$reasoningContent"
            content.isNotEmpty() -> content
            else -> ""
        }
        
        return StreamChunk(
            delta = ChatMessage(role = role, content = finalContent),
            finishReason = choiceObj.optString("finish_reason")
        )
    }
}
