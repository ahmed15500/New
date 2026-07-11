package com.ahmed.yawmeyaty.data

import android.content.Context
import com.ahmed.yawmeyaty.model.DailyTask
import com.ahmed.yawmeyaty.model.TaskCategory
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class TaskRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadTasks(): List<DailyTask> = defaultTasks + loadCustomTasks()

    fun addCustomTask(title: String, category: TaskCategory) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        val updated = loadCustomTasks().toMutableList().apply {
            add(
                DailyTask(
                    id = UUID.randomUUID().toString(),
                    title = cleanTitle,
                    category = category
                )
            )
        }
        saveCustomTasks(updated)
    }

    fun deleteCustomTask(taskId: String, date: LocalDate) {
        saveCustomTasks(loadCustomTasks().filterNot { it.id == taskId })
        val completed = loadCompletedIds(date).toMutableSet().apply { remove(taskId) }
        saveCompletedIds(date, completed)
    }

    fun loadCompletedIds(date: LocalDate): Set<String> =
        preferences.getStringSet(completionKey(date), emptySet())?.toSet().orEmpty()

    fun setTaskCompleted(taskId: String, completed: Boolean, date: LocalDate) {
        val updated = loadCompletedIds(date).toMutableSet()
        if (completed) updated.add(taskId) else updated.remove(taskId)
        saveCompletedIds(date, updated)
    }

    fun updateDailyRatio(date: LocalDate, totalTasks: Int, completedTasks: Int) {
        val ratio = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()
        preferences.edit().putFloat(ratioKey(date), ratio).apply()
    }

    fun calculateStreak(today: LocalDate): Int {
        var cursor = today
        if (preferences.getFloat(ratioKey(cursor), 0f) < STREAK_TARGET) {
            cursor = cursor.minusDays(1)
        }

        var streak = 0
        repeat(MAX_STREAK_LOOKBACK_DAYS) {
            val ratio = preferences.getFloat(ratioKey(cursor), 0f)
            if (ratio < STREAK_TARGET) return streak
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun loadCustomTasks(): List<DailyTask> {
        val json = preferences.getString(CUSTOM_TASKS_KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        DailyTask(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            category = runCatching {
                                TaskCategory.valueOf(item.getString("category"))
                            }.getOrDefault(TaskCategory.HABIT),
                            isDefault = false,
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveCustomTasks(tasks: List<DailyTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("category", task.category.name)
                    .put("createdAt", task.createdAt)
            )
        }
        preferences.edit().putString(CUSTOM_TASKS_KEY, array.toString()).apply()
    }

    private fun saveCompletedIds(date: LocalDate, ids: Set<String>) {
        preferences.edit().putStringSet(completionKey(date), ids.toSet()).apply()
    }

    private fun completionKey(date: LocalDate) = "completed_$date"
    private fun ratioKey(date: LocalDate) = "ratio_$date"

    companion object {
        private const val PREFS_NAME = "yawmeyaty_preferences"
        private const val CUSTOM_TASKS_KEY = "custom_tasks"
        private const val STREAK_TARGET = 0.70f
        private const val MAX_STREAK_LOOKBACK_DAYS = 365

        private val defaultTasks = listOf(
            DailyTask("fajr", "صلاة الفجر", TaskCategory.PRAYER, isDefault = true),
            DailyTask("morning_adhkar", "أذكار الصباح", TaskCategory.DHIKR, isDefault = true),
            DailyTask("dhuhr", "صلاة الظهر", TaskCategory.PRAYER, isDefault = true),
            DailyTask("asr", "صلاة العصر", TaskCategory.PRAYER, isDefault = true),
            DailyTask("maghrib", "صلاة المغرب", TaskCategory.PRAYER, isDefault = true),
            DailyTask("evening_adhkar", "أذكار المساء", TaskCategory.DHIKR, isDefault = true),
            DailyTask("isha", "صلاة العشاء", TaskCategory.PRAYER, isDefault = true),
            DailyTask("quran", "ورد القرآن", TaskCategory.QURAN, isDefault = true),
            DailyTask("good_deed", "صدقة أو عمل خير", TaskCategory.GOOD_DEED, isDefault = true),
            DailyTask("reflection", "محاسبة النفس قبل النوم", TaskCategory.HABIT, isDefault = true)
        )
    }
}
