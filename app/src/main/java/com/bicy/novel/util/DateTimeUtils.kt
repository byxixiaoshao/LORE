package com.bicy.novel.util

object DateTimeUtils {
    
    fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            diff < 604800_000 -> "${diff / 86400_000}天前"
            else -> {
                val date = java.util.Date(timestamp)
                val format = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                format.format(date)
            }
        }
    }
    
    fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    fun formatDateShort(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    fun formatDateTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}
