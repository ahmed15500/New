package com.ahmed.yawmeyaty.model

enum class TaskCategory(val label: String, val emoji: String) {
    PRAYER("الصلاة", "🕌"),
    QURAN("القرآن", "📖"),
    DHIKR("الذكر", "📿"),
    GOOD_DEED("عمل خير", "🤲"),
    HABIT("عادة نافعة", "🌱")
}

data class DailyTask(
    val id: String,
    val title: String,
    val category: TaskCategory,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
