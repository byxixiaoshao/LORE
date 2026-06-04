package com.bicy.novel.data.agent

import org.json.JSONObject

class RewriteSelectionTool : AgentTool {
    
    override val name = "rewrite_selection"
    override val description = "改写选中的文本内容。可以改变风格、语气、长度等。需要先选中文字。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "style" to ToolProperty("string", "改写风格：formal(正式), casual(随意), poetic(诗意), concise(简洁), expand(扩写)",
                enum = listOf("formal", "casual", "poetic", "concise", "expand")),
            "instruction" to ToolProperty("string", "额外的改写指令（可选）")
        ),
        required = listOf("style")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val selectedText = context.selectedText
            ?: return ToolResult.Error("请先选中要改写的文本")
        
        if (selectedText.isBlank()) {
            return ToolResult.Error("选中的文本为空")
        }
        
        val style = params.getString("style")
        val instruction = params.optString("instruction", "")
        
        val styleDesc = when (style) {
            "formal" -> "正式、严肃的风格"
            "casual" -> "轻松、随意的风格"
            "poetic" -> "诗意、文学化的风格"
            "concise" -> "简洁、精炼的风格"
            "expand" -> "扩写、增加细节"
            else -> style
        }
        
        return ToolResult.Success(
            message = "请改写以下文本：\n\n原文：\n$selectedText\n\n改写要求：$styleDesc${if (instruction.isNotBlank()) "\n额外要求：$instruction" else ""}",
            data = mapOf(
                "originalText" to selectedText,
                "style" to style,
                "instruction" to instruction
            )
        )
    }
}

class PolishSelectionTool : AgentTool {
    
    override val name = "polish_selection"
    override val description = "润色选中的文本内容，改善文字表达，使其更加流畅优美。需要先选中文字。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "focus" to ToolProperty("string", "润色重点：flow(流畅度), vocabulary(用词), grammar(语法), all(全部)",
                enum = listOf("flow", "vocabulary", "grammar", "all")),
            "instruction" to ToolProperty("string", "额外的润色指令（可选）")
        ),
        required = listOf("focus")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val selectedText = context.selectedText
            ?: return ToolResult.Error("请先选中要润色的文本")
        
        if (selectedText.isBlank()) {
            return ToolResult.Error("选中的文本为空")
        }
        
        val focus = params.getString("focus")
        val instruction = params.optString("instruction", "")
        
        val focusDesc = when (focus) {
            "flow" -> "改善文字流畅度和连贯性"
            "vocabulary" -> "优化用词，使表达更精准"
            "grammar" -> "修正语法错误和不通顺之处"
            "all" -> "全面润色"
            else -> focus
        }
        
        return ToolResult.Success(
            message = "请润色以下文本：\n\n原文：\n$selectedText\n\n润色重点：$focusDesc${if (instruction.isNotBlank()) "\n额外要求：$instruction" else ""}",
            data = mapOf(
                "originalText" to selectedText,
                "focus" to focus,
                "instruction" to instruction
            )
        )
    }
}

class GenerateOutlineTool : AgentTool {
    
    override val name = "generate_outline"
    override val description = "生成章节大纲。根据当前章节内容或用户描述生成结构化的大纲。"
    override val parameters = ToolParameters(
        properties = mapOf(
            "target" to ToolProperty("string", "大纲目标：current_chapter(当前章节), new_chapter(新章节), story(整体故事)",
                enum = listOf("current_chapter", "new_chapter", "story")),
            "description" to ToolProperty("string", "章节或故事的简要描述（可选，用于新章节或整体故事）"),
            "detail_level" to ToolProperty("string", "详细程度：brief(简略), normal(正常), detailed(详细)",
                enum = listOf("brief", "normal", "detailed"))
        ),
        required = listOf("target")
    )
    
    override suspend fun execute(params: JSONObject, context: ToolContext): ToolResult {
        val target = params.getString("target")
        val description = params.optString("description", "")
        val detailLevel = params.getString("detail")
        
        val targetDesc = when (target) {
            "current_chapter" -> "当前章节"
            "new_chapter" -> "新章节"
            "story" -> "整体故事"
            else -> target
        }
        
        val detailDesc = when (detailLevel) {
            "brief" -> "简略（3-5个要点）"
            "normal" -> "正常（5-10个要点）"
            "detailed" -> "详细（10个以上要点）"
            else -> detailLevel
        }
        
        return ToolResult.Success(
            message = "请生成${targetDesc}的大纲。\n\n详细程度：$detailDesc${if (description.isNotBlank()) "\n描述：$description" else ""}\n\n请以结构化的方式输出大纲，使用 Markdown 格式。",
            data = mapOf(
                "target" to target,
                "description" to description,
                "detailLevel" to detailLevel
            )
        )
    }
}
