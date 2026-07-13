package com.ahmed.yawmeyaty

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class CustomQuizQuestion(
    val id: String,
    val categoryTitle: String,
    val categoryEmoji: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String,
    val createdAt: Long = System.currentTimeMillis()
)

internal class CustomQuestionRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadQuestions(): List<CustomQuizQuestion> {
        val raw = preferences.getString(QUESTIONS_KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val optionsArray = item.optJSONArray("options") ?: JSONArray()
                    val options = buildList {
                        for (optionIndex in 0 until optionsArray.length()) {
                            add(optionsArray.optString(optionIndex))
                        }
                    }
                    if (options.size == 4) {
                        add(
                            CustomQuizQuestion(
                                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                                categoryTitle = item.optString("categoryTitle").ifBlank { "أسئلة متنوعة" },
                                categoryEmoji = item.optString("categoryEmoji").ifBlank { "✨" },
                                question = item.optString("question"),
                                options = options,
                                correctAnswer = item.optInt("correctAnswer", 0).coerceIn(0, 3),
                                explanation = item.optString("explanation"),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addQuestion(
        categoryTitle: String,
        categoryEmoji: String,
        question: String,
        options: List<String>,
        correctAnswer: Int,
        explanation: String
    ): Boolean {
        val cleanOptions = options.map(String::trim)
        if (
            categoryTitle.isBlank() ||
            question.isBlank() ||
            cleanOptions.size != 4 ||
            cleanOptions.any(String::isBlank) ||
            correctAnswer !in 0..3
        ) {
            return false
        }

        val updated = loadQuestions().toMutableList().apply {
            add(
                CustomQuizQuestion(
                    id = UUID.randomUUID().toString(),
                    categoryTitle = categoryTitle.trim(),
                    categoryEmoji = categoryEmoji.trim().ifBlank { "✨" },
                    question = question.trim(),
                    options = cleanOptions,
                    correctAnswer = correctAnswer,
                    explanation = explanation.trim().ifBlank {
                        "الإجابة الصحيحة هي: ${cleanOptions[correctAnswer]}"
                    }
                )
            )
        }
        saveQuestions(updated)
        return true
    }

    fun deleteQuestion(questionId: String) {
        saveQuestions(loadQuestions().filterNot { it.id == questionId })
    }

    fun exportPack(): String {
        val questions = loadQuestions()
        val array = JSONArray()
        questions.forEach { question ->
            array.put(question.toJson())
        }
        return JSONObject()
            .put("format", PACK_FORMAT)
            .put("version", 1)
            .put("title", "حزمة أسئلة إسلامية")
            .put("questions", array)
            .toString(2)
    }

    fun importPack(sharedText: String): Int {
        val jsonText = extractJson(sharedText)
        val root = JSONObject(jsonText)
        require(root.optString("format") == PACK_FORMAT) {
            "هذه ليست حزمة أسئلة صالحة للعبة."
        }

        val incoming = root.optJSONArray("questions") ?: JSONArray()
        val current = loadQuestions().toMutableList()
        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        var importedCount = 0

        for (index in 0 until incoming.length()) {
            val item = incoming.optJSONObject(index) ?: continue
            val optionsArray = item.optJSONArray("options") ?: continue
            if (optionsArray.length() != 4) continue

            val options = List(4) { optionIndex -> optionsArray.optString(optionIndex).trim() }
            val categoryTitle = item.optString("categoryTitle").trim()
            val questionText = item.optString("question").trim()
            val correctAnswer = item.optInt("correctAnswer", -1)
            if (
                categoryTitle.isBlank() ||
                questionText.isBlank() ||
                options.any(String::isBlank) ||
                correctAnswer !in 0..3
            ) {
                continue
            }

            val incomingId = item.optString("id").ifBlank { UUID.randomUUID().toString() }
            if (incomingId in existingIds) continue

            current.add(
                CustomQuizQuestion(
                    id = incomingId,
                    categoryTitle = categoryTitle,
                    categoryEmoji = item.optString("categoryEmoji").trim().ifBlank { "✨" },
                    question = questionText,
                    options = options,
                    correctAnswer = correctAnswer,
                    explanation = item.optString("explanation").trim().ifBlank {
                        "الإجابة الصحيحة هي: ${options[correctAnswer]}"
                    },
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            )
            existingIds.add(incomingId)
            importedCount += 1
        }

        if (importedCount > 0) saveQuestions(current)
        return importedCount
    }

    private fun saveQuestions(questions: List<CustomQuizQuestion>) {
        val array = JSONArray()
        questions.forEach { array.put(it.toJson()) }
        preferences.edit().putString(QUESTIONS_KEY, array.toString()).apply()
    }

    private fun CustomQuizQuestion.toJson(): JSONObject {
        val optionsArray = JSONArray()
        options.forEach(optionsArray::put)
        return JSONObject()
            .put("id", id)
            .put("categoryTitle", categoryTitle)
            .put("categoryEmoji", categoryEmoji)
            .put("question", question)
            .put("options", optionsArray)
            .put("correctAnswer", correctAnswer)
            .put("explanation", explanation)
            .put("createdAt", createdAt)
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "لم يتم العثور على بيانات الحزمة." }
        return text.substring(start, end + 1)
    }

    companion object {
        private const val PREFS_NAME = "islamic_quiz_custom_questions"
        private const val QUESTIONS_KEY = "questions"
        private const val PACK_FORMAT = "islamic_quiz_pack"
    }
}

internal fun customQuizCategories(questions: List<CustomQuizQuestion>): List<QuizCategory> =
    questions
        .groupBy { it.categoryTitle.trim() }
        .map { (title, categoryQuestions) ->
            QuizCategory(
                title = title,
                emoji = categoryQuestions.firstOrNull()?.categoryEmoji ?: "✨",
                description = "أسئلة أضافها المستخدمون",
                questions = categoryQuestions.map { question ->
                    QuizQuestion(
                        question = question.question,
                        options = question.options,
                        correctAnswer = question.correctAnswer,
                        explanation = question.explanation
                    )
                }
            )
        }
        .sortedBy { it.title }

@Composable
internal fun QuestionManagerEntryCard(questionCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("إنشاء وإرسال أسئلتك", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(
                    if (questionCount == 0) "أضف أول سؤال مخصّص" else "$questionCount سؤالًا مخصّصًا جاهزًا للمشاركة",
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomQuestionsScreen(
    repository: CustomQuestionRepository,
    onBack: () -> Unit,
    onQuestionsChanged: (List<CustomQuizQuestion>) -> Unit
) {
    val context = LocalContext.current
    var questions by remember { mutableStateOf(repository.loadQuestions()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        questions = repository.loadQuestions()
        onQuestionsChanged(questions)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "العودة")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("إدارة الأسئلة", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(
                        "أضف وشارك واستورد حزم الأسئلة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("أسئلتك المخصّصة", fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(
                            "${questions.size} سؤالًا في ${questions.map { it.categoryTitle }.distinct().size} أقسام",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة سؤال جديد", fontWeight = FontWeight.Black)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (questions.isEmpty()) {
                                message = "أضف سؤالًا واحدًا على الأقل قبل المشاركة."
                            } else {
                                shareQuestionPack(context, repository.exportPack())
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("مشاركة الحزمة")
                    }
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("استيراد حزمة")
                    }
                }
            }

            message?.let { currentMessage ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(
                            currentMessage,
                            modifier = Modifier.padding(14.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (questions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✍️", fontSize = 52.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("لم تضف أسئلة بعد", fontWeight = FontWeight.Black)
                        Text(
                            "أنشئ أسئلة بأي عدد ثم أرسلها للمستخدمين",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(questions.sortedByDescending { it.createdAt }, key = { it.id }) { question ->
                    CustomQuestionCard(
                        question = question,
                        onDelete = {
                            repository.deleteQuestion(question.id)
                            reload()
                            message = "تم حذف السؤال."
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomQuestionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { category, emoji, question, options, correctAnswer, explanation ->
                val saved = repository.addQuestion(
                    categoryTitle = category,
                    categoryEmoji = emoji,
                    question = question,
                    options = options,
                    correctAnswer = correctAnswer,
                    explanation = explanation
                )
                if (saved) {
                    reload()
                    showAddDialog = false
                    message = "تمت إضافة السؤال وأصبح جاهزًا للعب والمشاركة."
                }
            }
        )
    }

    if (showImportDialog) {
        ImportQuestionPackDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text ->
                runCatching { repository.importPack(text) }
                    .onSuccess { count ->
                        reload()
                        showImportDialog = false
                        message = if (count > 0) {
                            "تم استيراد $count سؤالًا بنجاح."
                        } else {
                            "لم تتم إضافة أسئلة جديدة؛ ربما تم استيراد الحزمة من قبل."
                        }
                    }
                    .onFailure { error ->
                        message = error.message ?: "تعذر استيراد الحزمة."
                    }
            }
        )
    }
}

@Composable
private fun CustomQuestionCard(question: CustomQuizQuestion, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(question.categoryEmoji, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(question.categoryTitle, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(question.question, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف السؤال",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "الإجابة: ${question.options[question.correctAnswer]}",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddCustomQuestionDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<String>, Int, String) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    var question by remember { mutableStateOf("") }
    var optionOne by remember { mutableStateOf("") }
    var optionTwo by remember { mutableStateOf("") }
    var optionThree by remember { mutableStateOf("") }
    var optionFour by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }

    val options = listOf(optionOne, optionTwo, optionThree, optionFour)
    val canSave = category.isNotBlank() && question.isNotBlank() && options.all(String::isNotBlank)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة سؤال جديد", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(3) },
                        modifier = Modifier.weight(0.3f),
                        label = { Text("رمز") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        modifier = Modifier.weight(0.7f),
                        label = { Text("اسم القسم") },
                        placeholder = { Text("مثال: الفقه") },
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("السؤال") },
                    minLines = 2
                )
                OutlinedTextField(value = optionOne, onValueChange = { optionOne = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاختيار الأول") })
                OutlinedTextField(value = optionTwo, onValueChange = { optionTwo = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاختيار الثاني") })
                OutlinedTextField(value = optionThree, onValueChange = { optionThree = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاختيار الثالث") })
                OutlinedTextField(value = optionFour, onValueChange = { optionFour = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاختيار الرابع") })

                Text("حدد الإجابة الصحيحة", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(4) { index ->
                        FilterChip(
                            selected = correctAnswer == index,
                            onClick = { correctAnswer = index },
                            label = { Text("${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("شرح الإجابة — اختياري") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(category, emoji, question, options, correctAnswer, explanation)
                }
            ) {
                Text("حفظ السؤال")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun ImportQuestionPackDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var packText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("استيراد حزمة أسئلة", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "انسخ الرسالة التي وصلتك عبر واتساب أو البريد والصقها هنا كاملة.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = packText,
                    onValueChange = { packText = it },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    label = { Text("نص الحزمة") }
                )
            }
        },
        confirmButton = {
            TextButton(enabled = packText.isNotBlank(), onClick = { onImport(packText) }) {
                Text("استيراد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

private fun shareQuestionPack(context: Context, json: String) {
    val shareText = buildString {
        appendLine("حزمة أسئلة للعبة اختبر معلوماتك الإسلامية")
        appendLine("انسخ هذه الرسالة كاملة، ثم افتح اللعبة واختر: إنشاء وإرسال أسئلتك ← استيراد حزمة.")
        appendLine()
        append(json)
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "حزمة أسئلة إسلامية")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "إرسال حزمة الأسئلة"))
}
