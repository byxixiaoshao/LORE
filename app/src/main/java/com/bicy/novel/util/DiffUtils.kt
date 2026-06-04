package com.bicy.novel.util

/**
 * 文本差异对比工具
 * 使用简单的逐行对比算法
 */
object DiffUtils {
    
    /**
     * 计算两个文本的差异
     * @return 差异行列表
     */
    fun diff(oldText: String, newText: String): List<DiffLine> {
        val oldLines = oldText.lines()
        val newLines = newText.lines()
        
        val result = mutableListOf<DiffLine>()
        val maxLines = maxOf(oldLines.size, newLines.size)
        
        for (i in 0 until maxLines) {
            val oldLine = oldLines.getOrNull(i)
            val newLine = newLines.getOrNull(i)
            
            when {
                oldLine == null && newLine != null -> {
                    // 新增行
                    result.add(DiffLine(
                        lineNumber = i + 1,
                        type = DiffType.ADDED,
                        content = newLine,
                        oldContent = null,
                        newContent = newLine
                    ))
                }
                oldLine != null && newLine == null -> {
                    // 删除行
                    result.add(DiffLine(
                        lineNumber = i + 1,
                        type = DiffType.REMOVED,
                        content = oldLine,
                        oldContent = oldLine,
                        newContent = null
                    ))
                }
                oldLine != null && newLine != null -> {
                    if (oldLine == newLine) {
                        // 未改变
                        result.add(DiffLine(
                            lineNumber = i + 1,
                            type = DiffType.UNCHANGED,
                            content = oldLine,
                            oldContent = oldLine,
                            newContent = newLine
                        ))
                    } else {
                        // 修改行
                        result.add(DiffLine(
                            lineNumber = i + 1,
                            type = DiffType.MODIFIED,
                            content = "$oldLine → $newLine",
                            oldContent = oldLine,
                            newContent = newLine
                        ))
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * 生成差异统计信息
     */
    fun getDiffStats(diffLines: List<DiffLine>): DiffStats {
        var added = 0
        var removed = 0
        var modified = 0
        var unchanged = 0
        
        diffLines.forEach { line ->
            when (line.type) {
                DiffType.ADDED -> added++
                DiffType.REMOVED -> removed++
                DiffType.MODIFIED -> modified++
                DiffType.UNCHANGED -> unchanged++
            }
        }
        
        return DiffStats(
            addedLines = added,
            removedLines = removed,
            modifiedLines = modified,
            unchangedLines = unchanged,
            totalChanges = added + removed + modified
        )
    }
}

/**
 * 差异类型
 */
enum class DiffType {
    UNCHANGED,  // 未改变
    ADDED,      // 新增
    REMOVED,    // 删除
    MODIFIED    // 修改
}

/**
 * 差异行
 */
data class DiffLine(
    val lineNumber: Int,
    val type: DiffType,
    val content: String,
    val oldContent: String?,
    val newContent: String?
)

/**
 * 差异统计
 */
data class DiffStats(
    val addedLines: Int,
    val removedLines: Int,
    val modifiedLines: Int,
    val unchangedLines: Int,
    val totalChanges: Int
)
