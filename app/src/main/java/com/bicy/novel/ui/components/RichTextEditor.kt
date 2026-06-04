package com.bicy.novel.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

enum class AILineChangeType {
    NONE,
    ADD,
    DELETE,
    MODIFY
}

data class AILineChange(
    val lineNumber: Int,
    val type: AILineChangeType,
    val originalContent: String? = null,
    val newContent: String? = null
)

data class AIEditPreview(
    val changes: List<AILineChange> = emptyList(),
    val description: String = "",
    val isApplying: Boolean = false
)

data class SearchResult(
    val startOffset: Int,
    val endOffset: Int,
    val lineNumber: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RichTextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
    searchQuery: String = "",
    currentSearchIndex: Int = 0,
    aiEditPreview: AIEditPreview = AIEditPreview(),
    showLineNumbers: Boolean = true,
    showAIPreview: Boolean = false,
    enabled: Boolean = true,
    autoWrap: Boolean = true,
    onSelectionChange: ((String) -> Unit)? = null
) {
    var editorSize by remember { mutableStateOf(IntSize.Zero) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    val searchResults = remember(text, searchQuery) {
        if (searchQuery.isEmpty()) emptyList()
        else {
            val results = mutableListOf<SearchResult>()
            var index = text.indexOf(searchQuery)
            val lines = text.lines()
            var currentOffset = 0
            
            while (index >= 0) {
                var lineNum = 0
                var lineStartOffset = 0
                for (i in lines.indices) {
                    if (currentOffset + lines[i].length >= index) {
                        lineNum = i
                        lineStartOffset = currentOffset
                        break
                    }
                    currentOffset += lines[i].length + 1
                }
                
                results.add(SearchResult(index, index + searchQuery.length, lineNum))
                index = text.indexOf(searchQuery, index + 1)
            }
            results
        }
    }
    
    val errorColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    
    val annotatedText = remember(text, searchQuery, searchResults, currentSearchIndex, aiEditPreview, showAIPreview, errorColor, primaryColor, tertiaryColor, surfaceVariant, onSurface) {
        buildAnnotatedString {
            if (showAIPreview && aiEditPreview.changes.isNotEmpty()) {
                val lines = text.lines()
                
                lines.forEachIndexed { lineIndex, line ->
                    val change = aiEditPreview.changes.find { it.lineNumber == lineIndex + 1 }
                    
                    if (change != null) {
                        when (change.type) {
                            AILineChangeType.DELETE -> {
                                withStyle(style = SpanStyle(
                                    background = errorColor.copy(alpha = 0.2f),
                                    color = errorColor,
                                    textDecoration = TextDecoration.LineThrough
                                )) {
                                    append(line)
                                }
                            }
                            AILineChangeType.MODIFY -> {
                                withStyle(style = SpanStyle(
                                    background = tertiaryColor.copy(alpha = 0.2f)
                                )) {
                                    append(line)
                                }
                            }
                            else -> {
                                append(line)
                            }
                        }
                    } else {
                        append(line)
                    }
                    
                    if (lineIndex < lines.size - 1) {
                        append("\n")
                    }
                }
            } else {
                append(text)
            }
            
            if (searchQuery.isNotEmpty()) {
                searchResults.forEachIndexed { index, result ->
                    val style = if (index == currentSearchIndex) {
                        SpanStyle(
                            background = primaryColor.copy(alpha = 0.4f),
                            color = onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        SpanStyle(
                            background = primaryColor.copy(alpha = 0.2f),
                            color = onSurface
                        )
                    }
                    addStyle(style, result.startOffset, result.endOffset)
                }
            }
        }
    }
    
    val visualLineInfo = remember(textLayoutResult, text, autoWrap) {
        if (autoWrap && textLayoutResult != null) {
            val layout = textLayoutResult!!
            val lineCount = layout.lineCount
            val logicalLines = text.lines()
            val logicalLineForVisualLine = mutableListOf<Int>()
            
            var currentLogicalLine = 0
            var lastLineEnd = 0
            
            for (visualLine in 0 until lineCount) {
                val lineStart = layout.getLineStart(visualLine)
                
                while (currentLogicalLine < logicalLines.size) {
                    val logicalLineEnd = lastLineEnd + logicalLines[currentLogicalLine].length
                    if (lineStart <= logicalLineEnd || currentLogicalLine == logicalLines.size - 1) {
                        logicalLineForVisualLine.add(currentLogicalLine)
                        break
                    }
                    currentLogicalLine++
                    lastLineEnd = logicalLineEnd + 1
                }
                
                if (currentLogicalLine >= logicalLines.size) {
                    logicalLineForVisualLine.add(logicalLines.size - 1)
                }
            }
            
            logicalLineForVisualLine
        } else {
            emptyList()
        }
    }
    
    val editorTextStyle = TextStyle(
        fontSize = fontSize.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = (fontSize + 8).sp
    )
    
    val density = LocalDensity.current
    val editorLineHeightDp = remember(fontSize, density) {
        with(density) { editorTextStyle.lineHeight.toDp() }
    }

    Row(modifier = modifier) {
        val scrollState = rememberScrollState()
        val hScrollState = rememberScrollState()

        if (showLineNumbers) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .verticalScroll(scrollState)
                    .padding(top = 8.dp, bottom = 8.dp, end = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                
                val logicalLines = text.lines()

                if (autoWrap && visualLineInfo.isNotEmpty()) {
                    var lastLogicalLine = -1

                    visualLineInfo.forEach { logicalIndex ->
                        val isNewLogicalLine = logicalIndex != lastLogicalLine
                        lastLogicalLine = logicalIndex

                        if (isNewLogicalLine) {
                            Text(
                                text = "${logicalIndex + 1}",
                                style = editorTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(editorLineHeightDp))
                        }
                    }
                } else {
                    logicalLines.forEachIndexed { logicalIndex, _ ->
                        Text(
                            text = "${logicalIndex + 1}",
                            style = editorTextStyle.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onGloballyPositioned { coordinates ->
                    editorSize = coordinates.size
                }
        ) {
            var currentSelection by remember { mutableStateOf<TextRange?>(null) }
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            
            // 光标移动时自动滚动到可见区域
            LaunchedEffect(currentSelection) {
                currentSelection?.let { selection ->
                    if (selection.collapsed) {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (autoWrap) {
                            Modifier.verticalScroll(scrollState)
                        } else {
                            Modifier
                                .verticalScroll(scrollState)
                                .horizontalScroll(hScrollState)
                        }
                    )
                    .padding(8.dp)
            ) {
                BasicTextField(
                    value = TextFieldValue(annotatedText, currentSelection ?: TextRange.Zero),
                    onValueChange = { newValue: TextFieldValue ->
                        // 正确处理 IME composing 状态
                        // 只有当 composing 结束时才更新文本
                        val isComposing = newValue.composition != null
                        
                        if (!isComposing && newValue.text != text) {
                            // composing 结束且文本变化，才触发更新
                            onTextChange(newValue.text)
                        }
                        
                        currentSelection = newValue.selection

                        if (onSelectionChange != null && newValue.selection.start != newValue.selection.end) {
                            val selectedText = newValue.text.substring(
                                newValue.selection.start.coerceIn(0, newValue.text.length),
                                newValue.selection.end.coerceIn(0, newValue.text.length)
                            )
                            if (selectedText.isNotBlank()) {
                                onSelectionChange(selectedText)
                            }
                        }
                    },
                    modifier = Modifier
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .then(
                            if (autoWrap) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier.wrapContentWidth()
                            }
                        ),
                    textStyle = editorTextStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    enabled = enabled,
                    onTextLayout = { layoutResult ->
                        textLayoutResult = layoutResult
                    },
                    decorationBox = { innerTextField: @Composable () -> Unit ->
                        if (text.isEmpty()) {
                            Text(
                                "开始写作...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = fontSize.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
            
            if (showAIPreview && aiEditPreview.changes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    aiEditPreview.changes
                        .filter { it.type == AILineChangeType.ADD && it.newContent != null }
                        .forEach { change ->
                            AINewLineIndicator(
                                lineNumber = change.lineNumber,
                                content = change.newContent ?: "",
                                fontSize = fontSize
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun AINewLineIndicator(
    lineNumber: Int,
    content: String,
    fontSize: Int
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        color = primaryColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "AI新增",
                modifier = Modifier.size(14.dp),
                tint = primaryColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                content,
                style = TextStyle(
                    fontSize = fontSize.sp,
                    color = primaryColor
                )
            )
        }
    }
}

@Composable
fun AIEditPreviewPanel(
    preview: AIEditPreview,
    onApply: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (preview.changes.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = "AI建议",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AI 修改建议",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                if (preview.isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onReject) {
                            Text("拒绝")
                        }
                        Button(onClick = onApply) {
                            Text("应用")
                        }
                    }
                }
            }
            
            if (preview.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    preview.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val addCount = preview.changes.count { it.type == AILineChangeType.ADD }
            val deleteCount = preview.changes.count { it.type == AILineChangeType.DELETE }
            val modifyCount = preview.changes.count { it.type == AILineChangeType.MODIFY }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (addCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "+$addCount 行",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (deleteCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "-$deleteCount 行",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (modifyCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "~$modifyCount 行",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AIDiffView(
    originalLines: List<String>,
    modifiedLines: List<String>,
    changes: List<AILineChange>,
    modifier: Modifier = Modifier
) {
    val maxLines = max(originalLines.size, modifiedLines.size)
    val lineIndices = (0 until maxLines).toList()
    
    LazyColumn(
        modifier = modifier
    ) {
        items(lineIndices) { index ->
            val lineNumber = index + 1
            val change = changes.find { it.lineNumber == lineNumber }
            val originalLine = originalLines.getOrNull(index) ?: ""
            val modifiedLine = modifiedLines.getOrNull(index) ?: ""
            
            when (change?.type) {
                AILineChangeType.DELETE -> {
                    DiffLine(
                        lineNumber = lineNumber,
                        content = originalLine,
                        type = AILineChangeType.DELETE,
                        showLine = true
                    )
                }
                AILineChangeType.ADD -> {
                    DiffLine(
                        lineNumber = lineNumber,
                        content = modifiedLine,
                        type = AILineChangeType.ADD,
                        showLine = true
                    )
                }
                AILineChangeType.MODIFY -> {
                    DiffLine(
                        lineNumber = lineNumber,
                        content = originalLine,
                        type = AILineChangeType.DELETE,
                        showLine = true
                    )
                    DiffLine(
                        lineNumber = lineNumber,
                        content = modifiedLine,
                        type = AILineChangeType.ADD,
                        showLine = false
                    )
                }
                else -> {
                    DiffLine(
                        lineNumber = lineNumber,
                        content = originalLine,
                        type = AILineChangeType.NONE,
                        showLine = true
                    )
                }
            }
        }
    }
}

@Composable
fun DiffLine(
    lineNumber: Int,
    content: String,
    type: AILineChangeType,
    showLine: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    val backgroundColor = when (type) {
        AILineChangeType.ADD -> primaryColor.copy(alpha = 0.1f)
        AILineChangeType.DELETE -> errorColor.copy(alpha = 0.1f)
        AILineChangeType.MODIFY -> tertiaryColor.copy(alpha = 0.1f)
        AILineChangeType.NONE -> Color.Transparent
    }
    
    val textColor = when (type) {
        AILineChangeType.ADD -> primaryColor
        AILineChangeType.DELETE -> errorColor
        AILineChangeType.MODIFY -> tertiaryColor
        AILineChangeType.NONE -> MaterialTheme.colorScheme.onSurface
    }
    
    val prefix = when (type) {
        AILineChangeType.ADD -> "+ "
        AILineChangeType.DELETE -> "- "
        AILineChangeType.MODIFY -> "~ "
        AILineChangeType.NONE -> "  "
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        if (showLine) {
            Text(
                "$lineNumber",
                modifier = Modifier.width(32.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        } else {
            Spacer(modifier = Modifier.width(32.dp))
        }
        
        Text(
            prefix,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            content,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}
