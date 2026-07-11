package com.ahmed.yawmeyaty.model

enum class WorkProject(val label: String, val emoji: String) {
    VILLAGES_13("مشروع القرى الـ13", "🏘️"),
    WASTE("إدارة المخلفات", "♻️"),
    ARTS("Arts for Climate", "🎨"),
    SUMMER_SCHOOL("المدرسة الصيفية", "🏫"),
    GWS_SENCE("GWS-SENCE", "💧"),
    SUSTAINABILITY_WEEK("Sustainability Week", "🌍"),
    GENERAL("عام", "📌")
}

enum class TaskPriority(val label: String, val rank: Int) {
    HIGH("عاجلة", 3),
    MEDIUM("متوسطة", 2),
    LOW("منخفضة", 1)
}

data class WorkTask(
    val id: String,
    val title: String,
    val project: WorkProject,
    val priority: TaskPriority,
    val dueDate: String?,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class FieldNote(
    val id: String,
    val village: String,
    val project: WorkProject,
    val note: String,
    val beneficiaries: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExpenseEntry(
    val id: String,
    val description: String,
    val amount: Double,
    val project: WorkProject,
    val createdAt: Long = System.currentTimeMillis()
)
