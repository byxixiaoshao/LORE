package com.bicy.novel.data.agent

import org.json.JSONObject

interface AgentTool {
    val name: String
    val description: String
    val parameters: ToolParameters
    
    suspend fun execute(params: JSONObject, context: ToolContext): ToolResult
}

data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String> = emptyList()
)

data class ToolProperty(
    val type: String,
    val description: String,
    val enum: List<String>? = null
)

data class ToolContext(
    val novelId: Long,
    val currentChapterId: Long? = null,
    val selectedText: String? = null,
    val selectedRange: IntRange? = null
)

sealed class ToolResult {
    data class Success(
        val message: String,
        val data: Map<String, Any?> = emptyMap(),
        val operationType: String? = null,
        val targetType: String? = null,
        val targetId: Long? = null,
        val targetName: String? = null,
        val beforeData: String? = null,
        val afterData: String? = null
    ) : ToolResult()
    
    data class Error(val message: String) : ToolResult()
    
    fun hasOperation(): Boolean {
        return this is Success && operationType != null && targetType != null && targetId != null
    }
}

fun AgentTool.toOpenAITool(): Map<String, Any> {
    return mapOf(
        "type" to "function",
        "function" to mapOf(
            "name" to name,
            "description" to description,
            "parameters" to mapOf(
                "type" to parameters.type,
                "properties" to parameters.properties.mapValues { (_, prop) ->
                    val map = mutableMapOf<String, Any>("type" to prop.type, "description" to prop.description)
                    prop.enum?.let { map["enum"] = it }
                    map
                },
                "required" to parameters.required
            )
        )
    )
}
