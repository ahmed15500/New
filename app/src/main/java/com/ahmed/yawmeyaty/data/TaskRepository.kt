package com.ahmed.yawmeyaty.data

import android.content.Context
import com.ahmed.yawmeyaty.model.ExpenseEntry
import com.ahmed.yawmeyaty.model.FieldNote
import com.ahmed.yawmeyaty.model.TaskPriority
import com.ahmed.yawmeyaty.model.WorkProject
import com.ahmed.yawmeyaty.model.WorkTask
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class TaskRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadTasks(): List<WorkTask> {
        val raw = preferences.getString(TASKS_KEY, null)
        if (raw == null) {
            val seeded = starterTasks()
            saveTasks(seeded)
            return seeded
        }
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        WorkTask(
                            id = item.getString("id"),
                            title = item.getString("title"),
                            project = enumValueOrDefault(
                                item.optString("project"),
                                WorkProject.GENERAL
                            ),
                            priority = enumValueOrDefault(
                                item.optString("priority"),
                                TaskPriority.MEDIUM
                            ),
                            dueDate = item.optString("dueDate").takeIf { it.isNotBlank() },
                            completed = item.optBoolean("completed", false),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addTask(
        title: String,
        project: WorkProject,
        priority: TaskPriority,
        dueDate: LocalDate?
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val updated = loadTasks().toMutableList().apply {
            add(
                WorkTask(
                    id = UUID.randomUUID().toString(),
                    title = cleanTitle,
                    project = project,
                    priority = priority,
                    dueDate = dueDate?.toString()
                )
            )
        }
        saveTasks(updated)
    }

    fun toggleTask(taskId: String) {
        saveTasks(
            loadTasks().map { task ->
                if (task.id == taskId) task.copy(completed = !task.completed) else task
            }
        )
    }

    fun deleteTask(taskId: String) {
        saveTasks(loadTasks().filterNot { it.id == taskId })
    }

    fun loadFieldNotes(): List<FieldNote> {
        val raw = preferences.getString(FIELD_NOTES_KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        FieldNote(
                            id = item.getString("id"),
                            village = item.optString("village"),
                            project = enumValueOrDefault(
                                item.optString("project"),
                                WorkProject.GENERAL
                            ),
                            note = item.optString("note"),
                            beneficiaries = item.optInt("beneficiaries", 0),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addFieldNote(
        village: String,
        project: WorkProject,
        note: String,
        beneficiaries: Int
    ) {
        val cleanVillage = village.trim()
        val cleanNote = note.trim()
        if (cleanVillage.isEmpty() || cleanNote.isEmpty()) return

        val updated = loadFieldNotes().toMutableList().apply {
            add(
                FieldNote(
                    id = UUID.randomUUID().toString(),
                    village = cleanVillage,
                    project = project,
                    note = cleanNote,
                    beneficiaries = beneficiaries.coerceAtLeast(0)
                )
            )
        }
        saveFieldNotes(updated)
    }

    fun deleteFieldNote(noteId: String) {
        saveFieldNotes(loadFieldNotes().filterNot { it.id == noteId })
    }

    fun loadExpenses(): List<ExpenseEntry> {
        val raw = preferences.getString(EXPENSES_KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        ExpenseEntry(
                            id = item.getString("id"),
                            description = item.optString("description"),
                            amount = item.optDouble("amount", 0.0),
                            project = enumValueOrDefault(
                                item.optString("project"),
                                WorkProject.GENERAL
                            ),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addExpense(description: String, amount: Double, project: WorkProject) {
        val cleanDescription = description.trim()
        if (cleanDescription.isEmpty() || amount <= 0.0) return
        val updated = loadExpenses().toMutableList().apply {
            add(
                ExpenseEntry(
                    id = UUID.randomUUID().toString(),
                    description = cleanDescription,
                    amount = amount,
                    project = project
                )
            )
        }
        saveExpenses(updated)
    }

    fun deleteExpense(expenseId: String) {
        saveExpenses(loadExpenses().filterNot { it.id == expenseId })
    }

    private fun saveTasks(tasks: List<WorkTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("id", task.id)
                    .put("title", task.title)
                    .put("project", task.project.name)
                    .put("priority", task.priority.name)
                    .put("dueDate", task.dueDate.orEmpty())
                    .put("completed", task.completed)
                    .put("createdAt", task.createdAt)
            )
        }
        preferences.edit().putString(TASKS_KEY, array.toString()).apply()
    }

    private fun saveFieldNotes(notes: List<FieldNote>) {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("village", note.village)
                    .put("project", note.project.name)
                    .put("note", note.note)
                    .put("beneficiaries", note.beneficiaries)
                    .put("createdAt", note.createdAt)
            )
        }
        preferences.edit().putString(FIELD_NOTES_KEY, array.toString()).apply()
    }

    private fun saveExpenses(expenses: List<ExpenseEntry>) {
        val array = JSONArray()
        expenses.forEach { expense ->
            array.put(
                JSONObject()
                    .put("id", expense.id)
                    .put("description", expense.description)
                    .put("amount", expense.amount)
                    .put("project", expense.project.name)
                    .put("createdAt", expense.createdAt)
            )
        }
        preferences.edit().putString(EXPENSES_KEY, array.toString()).apply()
    }

    private fun starterTasks(): List<WorkTask> {
        val today = LocalDate.now()
        return listOf(
            WorkTask(
                id = UUID.randomUUID().toString(),
                title = "تحديد أهم 3 أولويات لليوم",
                project = WorkProject.GENERAL,
                priority = TaskPriority.HIGH,
                dueDate = today.toString()
            ),
            WorkTask(
                id = UUID.randomUUID().toString(),
                title = "متابعة تحديثات مشروعات القرى الـ13",
                project = WorkProject.VILLAGES_13,
                priority = TaskPriority.MEDIUM,
                dueDate = today.toString()
            ),
            WorkTask(
                id = UUID.randomUUID().toString(),
                title = "مراجعة المصروفات والإيصالات",
                project = WorkProject.GENERAL,
                priority = TaskPriority.MEDIUM,
                dueDate = today.plusDays(1).toString()
            )
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    companion object {
        private const val PREFS_NAME = "midani_preferences"
        private const val TASKS_KEY = "work_tasks"
        private const val FIELD_NOTES_KEY = "field_notes"
        private const val EXPENSES_KEY = "expenses"
    }
}
