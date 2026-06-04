package com.bicy.novel.domain.model

data class Novel(
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val description: String = "",
    val coverPath: String? = null,
    val category: String = "",
    val status: Int = 0,
    val totalWords: Int = 0,
    val chapterCount: Int = 0,
    val lastEditedId: Long? = null,
    val lastEditedType: String = "CHAPTER",
    val isLocked: Boolean = false, // 项目是否被锁定
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_ONGOING = 0
        const val STATUS_COMPLETED = 1
        
        const val TYPE_CHAPTER = "CHAPTER"
        const val TYPE_CHARACTER = "CHARACTER"
        const val TYPE_WORLDVIEW = "WORLDVIEW"
        const val TYPE_NOTE = "NOTE"
        const val TYPE_TIMELINE = "TIMELINE"
    }
}
