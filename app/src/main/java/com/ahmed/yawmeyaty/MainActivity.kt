package com.ahmed.yawmeyaty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmed.yawmeyaty.data.TaskRepository
import com.ahmed.yawmeyaty.model.ExpenseEntry
import com.ahmed.yawmeyaty.model.FieldNote
import com.ahmed.yawmeyaty.model.TaskPriority
import com.ahmed.yawmeyaty.model.WorkProject
import com.ahmed.yawmeyaty.model.WorkTask
import com.ahmed.yawmeyaty.ui.theme.YawmeyatyTheme
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YawmeyatyTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MidaniApp(repository = remember { TaskRepository(applicationContext) })
                }
            }
        }
    }
}

private enum class AppSection(val label: String) {
    TODAY("اليوم"),
    FIELD("الميدان"),
    EXPENSES("المصروفات"),
    PROJECTS("المشروعات")
}

private enum class AddDialogType {
    TASK,
    FIELD_NOTE,
    EXPENSE
}

private enum class DueOption(val label: String, val daysFromToday: Long?) {
    TODAY("اليوم", 0),
    TOMORROW("غدًا", 1),
    THIS_WEEK("خلال أسبوع", 7),
    NO_DATE("بدون موعد", null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MidaniApp(repository: TaskRepository) {
    var section by remember { mutableStateOf(AppSection.TODAY) }
    var tasks by remember { mutableStateOf(repository.loadTasks()) }
    var notes by remember { mutableStateOf(repository.loadFieldNotes()) }
    var expenses by remember { mutableStateOf(repository.loadExpenses()) }
    var activeDialog by remember { mutableStateOf<AddDialogType?>(null) }

    fun reloadTasks() {
        tasks = repository.loadTasks()
    }

    fun reloadNotes() {
        notes = repository.loadFieldNotes()
    }

    fun reloadExpenses() {
        expenses = repository.loadExpenses()
    }

    val fabType = when (section) {
        AppSection.TODAY -> AddDialogType.TASK
        AppSection.FIELD -> AddDialogType.FIELD_NOTE
        AppSection.EXPENSES -> AddDialogType.EXPENSE
        AppSection.PROJECTS -> AddDialogType.TASK
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ميداني", fontWeight = FontWeight.Black)
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppSection.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == section,
                        onClick = { section = item },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    AppSection.TODAY -> Icons.Rounded.Home
                                    AppSection.FIELD -> Icons.Rounded.LocationOn
                                    AppSection.EXPENSES -> Icons.Rounded.AttachMoney
                                    AppSection.PROJECTS -> Icons.Rounded.Folder
                                },
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { activeDialog = fabType },
                modifier = Modifier.navigationBarsPadding(),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = {
                    Text(
                        when (fabType) {
                            AddDialogType.TASK -> "مهمة"
                            AddDialogType.FIELD_NOTE -> "زيارة"
                            AddDialogType.EXPENSE -> "مصروف"
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        when (section) {
            AppSection.TODAY -> TodayScreen(
                tasks = tasks,
                expenses = expenses,
                modifier = Modifier.padding(innerPadding),
                onToggle = { id ->
                    repository.toggleTask(id)
                    reloadTasks()
                },
                onDelete = { id ->
                    repository.deleteTask(id)
                    reloadTasks()
                }
            )

            AppSection.FIELD -> FieldScreen(
                notes = notes,
                modifier = Modifier.padding(innerPadding),
                onDelete = { id ->
                    repository.deleteFieldNote(id)
                    reloadNotes()
                }
            )

            AppSection.EXPENSES -> ExpensesScreen(
                expenses = expenses,
                modifier = Modifier.padding(innerPadding),
                onDelete = { id ->
                    repository.deleteExpense(id)
                    reloadExpenses()
                }
            )

            AppSection.PROJECTS -> ProjectsScreen(
                tasks = tasks,
                notes = notes,
                expenses = expenses,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    when (activeDialog) {
        AddDialogType.TASK -> AddTaskDialog(
            onDismiss = { activeDialog = null },
            onAdd = { title, project, priority, dueDate ->
                repository.addTask(title, project, priority, dueDate)
                reloadTasks()
                activeDialog = null
            }
        )

        AddDialogType.FIELD_NOTE -> AddFieldNoteDialog(
            onDismiss = { activeDialog = null },
            onAdd = { village, project, note, beneficiaries ->
                repository.addFieldNote(village, project, note, beneficiaries)
                reloadNotes()
                activeDialog = null
            }
        )

        AddDialogType.EXPENSE -> AddExpenseDialog(
            onDismiss = { activeDialog = null },
            onAdd = { description, amount, project ->
                repository.addExpense(description, amount, project)
                reloadExpenses()
                activeDialog = null
            }
        )

        null -> Unit
    }
}

@Composable
private fun TodayScreen(
    tasks: List<WorkTask>,
    expenses: List<ExpenseEntry>,
    modifier: Modifier = Modifier,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val today = LocalDate.now()
    val completed = tasks.count { it.completed }
    val pending = tasks.size - completed
    val todayExpenses = expenses
        .filter { epochToDate(it.createdAt) == today }
        .sumOf { it.amount }

    val sortedTasks = tasks.sortedWith(
        compareBy<WorkTask> { it.completed }
            .thenByDescending { it.priority.rank }
            .thenBy { it.dueDate ?: "9999-12-31" }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = arabicDate(today),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "ركّز على المهم وسجّل ما يحدث في الميدان",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "متبقي",
                    value = pending.toString(),
                    emoji = "⏳",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "مكتمل",
                    value = completed.toString(),
                    emoji = "✅",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "مصروف اليوم",
                    value = compactAmount(todayExpenses),
                    emoji = "💰",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionTitle(
                title = "مهامك",
                subtitle = if (pending > 0) "$pending مهام تحتاج متابعة" else "كل المهام مكتملة"
            )
        }

        if (sortedTasks.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🗂️",
                    title = "لا توجد مهام",
                    subtitle = "اضغط زر الإضافة وسجّل أول مهمة"
                )
            }
        } else {
            items(items = sortedTasks, key = { it.id }) { task ->
                WorkTaskCard(task = task, onToggle = onToggle, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkTaskCard(
    task: WorkTask,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val overdue = isOverdue(task.dueDate) && !task.completed
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
        TaskPriority.MEDIUM -> Color(0xFFD47700)
        TaskPriority.LOW -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.completed) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle(task.id) }
            )
            Spacer(Modifier.width(8.dp))
            Text(task.project.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (task.completed) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "${task.project.label} • ${task.priority.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = priorityColor
                )
                task.dueDate?.let { date ->
                    Text(
                        text = if (overdue) "متأخرة • ${dueDateLabel(date)}" else dueDateLabel(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onDelete(task.id) }) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "حذف المهمة",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Icon(
                imageVector = if (task.completed) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.RadioButtonUnchecked
                },
                contentDescription = null,
                tint = if (task.completed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}

@Composable
private fun FieldScreen(
    notes: List<FieldNote>,
    modifier: Modifier = Modifier,
    onDelete: (String) -> Unit
) {
    val totalBeneficiaries = notes.sumOf { it.beneficiaries }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "سجل الميدان",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "وثّق الزيارات والملاحظات وعدد المستفيدين فورًا",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = "زيارات مسجلة",
                    value = notes.size.toString(),
                    emoji = "📍",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "مستفيدون",
                    value = totalBeneficiaries.toString(),
                    emoji = "👥",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (notes.isEmpty()) {
            item {
                EmptyState(
                    emoji = "📝",
                    title = "لا توجد زيارات مسجلة",
                    subtitle = "سجّل القرية والمشروع وأهم ما حدث"
                )
            }
        } else {
            items(notes.sortedByDescending { it.createdAt }, key = { it.id }) { note ->
                FieldNoteCard(note = note, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun FieldNoteCard(note: FieldNote, onDelete: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📍", fontSize = 22.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(note.village, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${note.project.emoji} ${note.project.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onDelete(note.id) }) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف الزيارة",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(note.note, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = dateTimeLabel(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (note.beneficiaries > 0) {
                    Text(
                        text = "${note.beneficiaries} مستفيد",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpensesScreen(
    expenses: List<ExpenseEntry>,
    modifier: Modifier = Modifier,
    onDelete: (String) -> Unit
) {
    val total = expenses.sumOf { it.amount }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "المصروفات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "سجّل المصروف وقت حدوثه حتى لا تضيع الإيصالات",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("إجمالي المصروفات المسجلة", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatAmount(total),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🧾",
                    title = "لا توجد مصروفات",
                    subtitle = "أضف الوصف والمبلغ والمشروع"
                )
            }
        } else {
            items(expenses.sortedByDescending { it.createdAt }, key = { it.id }) { expense ->
                ExpenseCard(expense = expense, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun ExpenseCard(expense: ExpenseEntry, onDelete: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🧾", fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.Bold)
                Text(
                    "${expense.project.emoji} ${expense.project.label} • ${dateTimeLabel(expense.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatAmount(expense.amount),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = { onDelete(expense.id) }) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف المصروف",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectsScreen(
    tasks: List<WorkTask>,
    notes: List<FieldNote>,
    expenses: List<ExpenseEntry>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "لوحة المشروعات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "ملخص سريع للمهام والزيارات والمصروفات حسب المشروع",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(WorkProject.entries, key = { it.name }) { project ->
            val projectTasks = tasks.filter { it.project == project }
            val completedTasks = projectTasks.count { it.completed }
            val projectNotes = notes.count { it.project == project }
            val projectExpenses = expenses.filter { it.project == project }.sumOf { it.amount }
            val progress = if (projectTasks.isEmpty()) 0f
            else completedTasks.toFloat() / projectTasks.size.toFloat()

            ProjectCard(
                project = project,
                tasks = projectTasks.size,
                completed = completedTasks,
                visits = projectNotes,
                expenses = projectExpenses,
                progress = progress
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: WorkProject,
    tasks: Int,
    completed: Int,
    visits: Int,
    expenses: Double,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(project.emoji, fontSize = 30.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        "$completed من $tasks مهام مكتملة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(progress * 100).toInt()}٪",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("📍 $visits زيارات", style = MaterialTheme.typography.labelMedium)
                Text("💰 ${compactAmount(expenses)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            subtitle,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, WorkProject, TaskPriority, LocalDate?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var project by remember { mutableStateOf(WorkProject.GENERAL) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueOption by remember { mutableStateOf(DueOption.TODAY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مهمة", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("المهمة") },
                    placeholder = { Text("مثال: متابعة تقرير المشروع") },
                    singleLine = true
                )
                Spacer(Modifier.height(14.dp))
                SelectorLabel("المشروع")
                ProjectSelector(selected = project, onSelected = { project = it })
                Spacer(Modifier.height(14.dp))
                SelectorLabel("الأولوية")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { item ->
                        FilterChip(
                            selected = item == priority,
                            onClick = { priority = item },
                            label = { Text(item.label) }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                SelectorLabel("الموعد")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DueOption.entries.forEach { item ->
                        FilterChip(
                            selected = item == dueOption,
                            onClick = { dueOption = item },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val dueDate = dueOption.daysFromToday?.let { LocalDate.now().plusDays(it) }
                    onAdd(title, project, priority, dueDate)
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun AddFieldNoteDialog(
    onDismiss: () -> Unit,
    onAdd: (String, WorkProject, String, Int) -> Unit
) {
    var village by remember { mutableStateOf("") }
    var project by remember { mutableStateOf(WorkProject.VILLAGES_13) }
    var note by remember { mutableStateOf("") }
    var beneficiaries by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل زيارة ميدانية", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("القرية أو المكان") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                SelectorLabel("المشروع")
                ProjectSelector(selected = project, onSelected = { project = it })
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ماذا حدث؟") },
                    minLines = 3
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = beneficiaries,
                    onValueChange = { value ->
                        beneficiaries = value.filter { it.isDigit() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عدد المستفيدين — اختياري") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = village.isNotBlank() && note.isNotBlank(),
                onClick = {
                    onAdd(village, project, note, beneficiaries.toIntOrNull() ?: 0)
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, WorkProject) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var project by remember { mutableStateOf(WorkProject.GENERAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل مصروف", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("وصف المصروف") },
                    placeholder = { Text("مثال: انتقالات الزيارة") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        amount = value.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("المبلغ بالجنيه") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                SelectorLabel("المشروع")
                ProjectSelector(selected = project, onSelected = { project = it })
            }
        },
        confirmButton = {
            val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
            TextButton(
                enabled = description.isNotBlank() && parsedAmount != null && parsedAmount > 0,
                onClick = {
                    onAdd(description, parsedAmount ?: 0.0, project)
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun SelectorLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ProjectSelector(
    selected: WorkProject,
    onSelected: (WorkProject) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WorkProject.entries.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelected(item) },
                label = { Text("${item.emoji} ${item.label}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

private fun arabicDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar", "EG")))

private fun epochToDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun dateTimeLabel(epochMillis: Long): String {
    val value = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return value.format(DateTimeFormatter.ofPattern("d MMM، h:mm a", Locale("ar", "EG")))
}

private fun dueDateLabel(rawDate: String): String {
    val date = runCatching { LocalDate.parse(rawDate) }.getOrNull() ?: return rawDate
    val today = LocalDate.now()
    return when (date) {
        today -> "اليوم"
        today.plusDays(1) -> "غدًا"
        else -> date.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ar", "EG")))
    }
}

private fun isOverdue(rawDate: String?): Boolean {
    val date = rawDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
    return date.isBefore(LocalDate.now())
}

private fun formatAmount(amount: Double): String =
    "${NumberFormat.getNumberInstance(Locale("ar", "EG")).format(amount)} ج.م"

private fun compactAmount(amount: Double): String {
    if (amount <= 0.0) return "0"
    return when {
        amount >= 1_000_000 -> "${(amount / 1_000_000).toInt()}م"
        amount >= 1_000 -> "${(amount / 1_000).toInt()}ك"
        else -> NumberFormat.getNumberInstance(Locale("ar", "EG")).format(amount)
    }
}
