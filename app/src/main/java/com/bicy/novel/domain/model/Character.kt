package com.bicy.novel.domain.model

data class Character(
    val id: Long = 0,
    val novelId: Long,
    val name: String,
    val alias: String = "",
    val roleType: String = "",
    val description: String = "",
    val avatarPath: String? = null,
    val attributes: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_PROTAGONIST = "主角"
        const val ROLE_SUPPORTING = "配角"
        const val ROLE_ANTAGONIST = "反派"
        const val ROLE_OTHER = "其他"
    }
}
