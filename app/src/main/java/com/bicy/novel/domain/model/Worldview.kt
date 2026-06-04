package com.bicy.novel.domain.model

data class Worldview(
    val id: Long = 0,
    val novelId: Long,
    val title: String,
    val category: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_SETTING = "设定"
        const val CATEGORY_FACTION = "势力"
        const val CATEGORY_LOCATION = "地点"
        const val CATEGORY_SYSTEM = "体系"
        const val CATEGORY_OTHER = "其他"
    }
}
