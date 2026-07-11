package com.ahmed.yawmeyaty

import android.icu.util.IslamicCalendar
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.ahmed.yawmeyaty.model.DailyTask
import com.ahmed.yawmeyaty.model.TaskCategory
import com.ahmed.yawmeyaty.ui.theme.YawmeyatyTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YawmeyatyTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    YawmeyatyApp(repository = remember { TaskRepository(applicationContext) })
                }
            }
        }
    }
}

private enum class TaskFilter(val label: String) {
    ALL("الكل"),
    PENDING("المتبقي"),
    COMPLETED("المكتمل")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YawmeyatyApp(repository: TaskRepository) {
    val today = remember { LocalDate.now() }
    var tasks by remember { mutableStateOf(repository.loadTasks()) }
    var completedIds by remember { mutableStateOf(repository.loadCompletedIds(today)) }
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(repository.calculateStreak(today)) }

    fun updateProgressState(updatedTasks: List<DailyTask> = tasks, updatedIds: Set<String> = completedIds) {
        repository.updateDailyRatio(
            date = today,
            totalTasks = updatedTasks.size,
            completedTasks = updatedIds.count { id -> updatedTasks.any { it.id == id } }
        )
        streak = repository.calculateStreak(today)
    }

    val completedCount = completedIds.count { id -> tasks.any { it.id == id } }
    val progress = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size.toFloat()
    val visibleTasks = tasks.filter { task ->
        when (filter) {
            TaskFilter.ALL -> true
            TaskFilter.PENDING -> task.id !in completedIds
            TaskFilter.COMPLETED -> task.id in completedIds
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("إضافة مهمة") },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 108.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { WelcomeHeader(today = today) }
            item {
                ProgressCard(
                    completedCount = completedCount,
                    totalCount = tasks.size,
                    progress = progress,
                    streak = streak
                )
            }
            item { FilterBar(selected = filter, onSelected = { filter = it }) }

            if (visibleTasks.isEmpty()) {
                item { EmptyState(filter = filter) }
            } else {
                items(items = visibleTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        completed = task.id in completedIds,
                        onCheckedChange = { checked ->
                            repository.setTaskCompleted(task.id, checked, today)
                            completedIds = repository.loadCompletedIds(today)
                            updateProgressState(updatedIds = completedIds)
                        },
                        onDelete = if (task.isDefault) null else {
                            {
                                repository.deleteCustomTask(task.id, today)
                                tasks = repository.loadTasks()
                                completedIds = repository.loadCompletedIds(today)
                                updateProgressState(updatedTasks = tasks, updatedIds = completedIds)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, category ->
                repository.addCustomTask(title, category)
                tasks = repository.loadTasks()
                updateProgressState(updatedTasks = tasks)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun WelcomeHeader(today: LocalDate) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "يومياتي",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "خطوات صغيرة ثابتة تقرّبك إلى الله",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "☾", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = gregorianDate(today),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = hijriDate(), style = MaterialTheme.typography.bodyMedium)
                }
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(completedCount: Int, totalCount: Int, progress: Float, streak: Int) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إنجاز اليوم",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$completedCount من $totalCount مهام",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}٪",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(14.dp))
            AssistChip(
                onClick = {},
                label = {
                    Text(if (streak > 0) "$streak أيام متتالية فوق ٧٠٪" else "ابدأ سلسلة التزامك اليوم")
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFD26A00)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                )
            )
        }
    }
}

@Composable
private fun FilterBar(selected: TaskFilter, onSelected: (TaskFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskFilter.entries.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelected(item) },
                label = { Text(item.label) },
                leadingIcon = if (item == selected) {
                    { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: DailyTask,
    completed: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
            } else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (completed) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = completed, onCheckedChange = onCheckedChange)
            Spacer(Modifier.width(8.dp))
            Text(text = task.category.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = task.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف المهمة",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Icon(
                    imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun EmptyState(filter: TaskFilter) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (filter == TaskFilter.COMPLETED) "🌱" else "✨", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = when (filter) {
                TaskFilter.ALL -> "لا توجد مهام بعد"
                TaskFilter.PENDING -> "أحسنت، أنجزت كل مهام اليوم"
                TaskFilter.COMPLETED -> "ابدأ بأول مهمة وسيظهر إنجازك هنا"
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, TaskCategory) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.HABIT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مهمة إسلامية جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم المهمة") },
                    placeholder = { Text("مثال: حفظ خمس آيات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text("التصنيف", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskCategory.entries.forEach { item ->
                        FilterChip(
                            selected = item == category,
                            onClick = { category = item },
                            label = { Text("${item.emoji} ${item.label}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title, category) }, enabled = title.isNotBlank()) {
                Text("إضافة")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private fun gregorianDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
    return date.format(formatter)
}

private fun hijriDate(): String {
    val calendar = IslamicCalendar().apply {
        setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA)
        timeInMillis = System.currentTimeMillis()
    }
    val monthNames = listOf(
        "محرّم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوّال", "ذو القعدة", "ذو الحجة"
    )
    val day = calendar.get(IslamicCalendar.DAY_OF_MONTH)
    val month = monthNames[calendar.get(IslamicCalendar.MONTH)]
    val year = calendar.get(IslamicCalendar.YEAR)
    return "$day $month $year هـ"
}
