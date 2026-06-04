package com.bicy.novel.ui.components

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope

class StreamingMessageNativeView(
    context: Context,
    private val coroutineScope: CoroutineScope
) : LinearLayout(context) {

    private val containerColor = Color.parseColor("#1E1E1E")
    private val contentColor = Color.parseColor("#E0E0E0")
    private val tertiaryColor = Color.parseColor("#2D2D2D")
    private val onTertiaryColor = Color.parseColor("#B0B0B0")
    private val primaryColor = Color.parseColor("#4CAF50")
    private val errorColor = Color.parseColor("#F44336")
    
    private var currentMessage: StreamingMessageData? = null
    private var currentExpandedMap: Map<Int, Boolean> = emptyMap()
    private var onToggleExpand: ((Int) -> Unit)? = null
    
    private val contentContainer: LinearLayout
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private data class StreamingMessageData(
        val rounds: List<RoundData>,
        val content: String,
        val updateVersion: Long
    )
    
    private data class RoundData(
        val round: Int,
        val thinking: String,
        val toolCalls: List<ToolCallData>
    )
    
    private data class ToolCallData(
        val toolName: String,
        val result: String?,
        val isError: Boolean,
        val isComplete: Boolean
    )
    
    init {
        orientation = VERTICAL
        setPadding(24, 24, 24, 24)
        setBackgroundColor(containerColor)
        
        val headerLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
            
            addView(TextView(context).apply {
                text = "AI"
                textSize = 12f
                setTextColor(contentColor)
            })
            
            addView(View(context).apply {
                layoutParams = LayoutParams(0, 0, 1f)
            })
            
            addView(TextView(context).apply {
                text = "⏳"
                textSize = 14f
            })
        }
        addView(headerLayout)
        
        addView(View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 16)
        })
        
        contentContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        }
        addView(contentContainer)
    }
    
    fun setMessage(
        rounds: List<Triple<Int, String, List<ToolCallInfo>>>,
        content: String,
        updateVersion: Long
    ) {
        currentMessage = StreamingMessageData(
            rounds = rounds.map { (round, thinking, toolCalls) ->
                RoundData(
                    round = round,
                    thinking = thinking,
                    toolCalls = toolCalls.map { tc ->
                        ToolCallData(
                            toolName = tc.toolName,
                            result = tc.result,
                            isError = tc.isError,
                            isComplete = tc.isComplete
                        )
                    }
                )
            },
            content = content,
            updateVersion = updateVersion
        )
        postUpdate()
    }
    
    fun setExpandedMap(map: Map<Int, Boolean>) {
        currentExpandedMap = map
        postUpdate()
    }
    
    fun setOnToggleExpand(listener: (Int) -> Unit) {
        onToggleExpand = listener
    }
    
    private fun postUpdate() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateContent()
        } else {
            mainHandler.post { updateContent() }
        }
    }
    
    private fun updateContent() {
        contentContainer.removeAllViews()
        
        val msg = currentMessage ?: return
        
        msg.rounds.forEachIndexed { index, round ->
            if (round.thinking.isNotEmpty()) {
                val isExpanded = currentExpandedMap[index] ?: false
                
                val thinkingHeader = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    setPadding(16, 12, 16, 12)
                    setBackgroundColor(tertiaryColor)
                    
                    addView(TextView(context).apply {
                        text = "🧠 思考过程 (第${round.round}轮)"
                        textSize = 12f
                        setTextColor(onTertiaryColor)
                    })
                    
                    addView(View(context).apply {
                        layoutParams = LayoutParams(0, 0, 1f)
                    })
                    
                    addView(TextView(context).apply {
                        text = if (isExpanded) "▼" else "▶"
                        textSize = 12f
                        setTextColor(onTertiaryColor)
                        setOnClickListener {
                            onToggleExpand?.invoke(index)
                        }
                    })
                }
                contentContainer.addView(thinkingHeader)
                
                if (isExpanded) {
                    val thinkingContent = TextView(context).apply {
                        text = round.thinking
                        textSize = 12f
                        setTextColor(onTertiaryColor)
                        setPadding(16, 8, 16, 8)
                        setBackgroundColor(tertiaryColor)
                    }
                    contentContainer.addView(thinkingContent)
                }
                
                contentContainer.addView(View(context).apply {
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 12)
                })
            }
            
            if (round.toolCalls.isNotEmpty()) {
                val toolCallsContainer = LinearLayout(context).apply {
                    orientation = VERTICAL
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    setPadding(16, 12, 16, 12)
                    setBackgroundColor(tertiaryColor)
                    
                    addView(TextView(context).apply {
                        text = "🔧 工具调用"
                        textSize = 12f
                        setTextColor(onTertiaryColor)
                    })
                    
                    addView(View(context).apply {
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 8)
                    })
                    
                    round.toolCalls.forEach { toolCall ->
                        val toolName = when (toolCall.toolName) {
                            "create" -> "创建"
                            "edit" -> "编辑"
                            "delete" -> "删除"
                            "view" -> "查看"
                            else -> toolCall.toolName
                        }
                        
                        val statusIcon = when {
                            !toolCall.isComplete -> "⏳"
                            toolCall.isError -> "❌"
                            else -> "✓"
                        }
                        
                        val color = when {
                            !toolCall.isComplete -> onTertiaryColor
                            toolCall.isError -> errorColor
                            else -> primaryColor
                        }
                        
                        val resultText = if (toolCall.result != null && toolCall.isComplete) {
                            val r = toolCall.result
                            if (r.length > 50) r.take(50) + "..." else r
                        } else ""
                        
                        addView(TextView(context).apply {
                            text = "  $statusIcon $toolName${if (resultText.isNotEmpty()) " → $resultText" else ""}"
                            textSize = 12f
                            setTextColor(color)
                        })
                    }
                }
                contentContainer.addView(toolCallsContainer)
                
                contentContainer.addView(View(context).apply {
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 12)
                })
            }
        }
        
        if (msg.content.isNotEmpty()) {
            val contentText = TextView(context).apply {
                text = msg.content
                textSize = 14f
                setTextColor(contentColor)
            }
            contentContainer.addView(contentText)
        }
    }
}

data class ToolCallInfo(
    val toolName: String,
    val result: String? = null,
    val isError: Boolean = false,
    val isComplete: Boolean = false
)
