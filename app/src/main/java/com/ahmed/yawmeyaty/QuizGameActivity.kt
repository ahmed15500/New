package com.ahmed.yawmeyaty

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmed.yawmeyaty.ui.theme.YawmeyatyTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class QuizGameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YawmeyatyTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    IslamicQuizApp()
                }
            }
        }
    }
}

private enum class QuizView {
    HOME,
    QUIZ,
    RESULT,
    MANAGE
}

private data class GameQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

private data class GameCategory(
    val title: String,
    val emoji: String,
    val description: String,
    val questions: List<GameQuestion>
)

private data class StoredQuestion(
    val id: String,
    val categoryTitle: String,
    val categoryEmoji: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String,
    val createdAt: Long = System.currentTimeMillis()
)

private class QuestionPackStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<StoredQuestion> {
        val raw = preferences.getString(QUESTIONS_KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val optionsArray = item.optJSONArray("options") ?: continue
                    if (optionsArray.length() != 4) continue
                    val options = List(4) { optionsArray.optString(it).trim() }
                    val questionText = item.optString("question").trim()
                    val categoryTitle = item.optString("categoryTitle").trim()
                    val correct = item.optInt("correctAnswer", -1)
                    if (
                        questionText.isBlank() ||
                        categoryTitle.isBlank() ||
                        options.any { it.isBlank() } ||
                        correct !in 0..3
                    ) {
                        continue
                    }
                    add(
                        StoredQuestion(
                            id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                            categoryTitle = categoryTitle,
                            categoryEmoji = item.optString("categoryEmoji").trim().ifBlank { "✨" },
                            question = questionText,
                            options = options,
                            correctAnswer = correct,
                            explanation = item.optString("explanation").trim().ifBlank {
                                "الإجابة الصحيحة هي: ${options[correct]}"
                            },
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(
        categoryTitle: String,
        categoryEmoji: String,
        question: String,
        options: List<String>,
        correctAnswer: Int,
        explanation: String
    ): Boolean {
        val cleanOptions = options.map { it.trim() }
        if (
            categoryTitle.isBlank() ||
            question.isBlank() ||
            cleanOptions.size != 4 ||
            cleanOptions.any { it.isBlank() } ||
            correctAnswer !in 0..3
        ) {
            return false
        }

        val updated = load().toMutableList().apply {
            add(
                StoredQuestion(
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
        save(updated)
        return true
    }

    fun delete(questionId: String) {
        save(load().filterNot { it.id == questionId })
    }

    fun exportPack(): String {
        val array = JSONArray()
        load().forEach { array.put(it.toJson()) }
        return JSONObject()
            .put("format", PACK_FORMAT)
            .put("version", 1)
            .put("title", "حزمة أسئلة إسلامية")
            .put("questions", array)
            .toString(2)
    }

    fun importPack(sharedText: String): Int {
        val root = JSONObject(extractJson(sharedText))
        require(root.optString("format") == PACK_FORMAT) {
            "هذه ليست حزمة أسئلة صالحة."
        }

        val incoming = root.optJSONArray("questions") ?: JSONArray()
        val current = load().toMutableList()
        val existingIds = current.mapTo(mutableSetOf()) { it.id }
        var imported = 0

        for (index in 0 until incoming.length()) {
            val item = incoming.optJSONObject(index) ?: continue
            val optionsArray = item.optJSONArray("options") ?: continue
            if (optionsArray.length() != 4) continue

            val options = List(4) { optionsArray.optString(it).trim() }
            val category = item.optString("categoryTitle").trim()
            val text = item.optString("question").trim()
            val correct = item.optInt("correctAnswer", -1)
            if (
                category.isBlank() ||
                text.isBlank() ||
                options.any { it.isBlank() } ||
                correct !in 0..3
            ) {
                continue
            }

            val id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
            if (id in existingIds) continue

            current.add(
                StoredQuestion(
                    id = id,
                    categoryTitle = category,
                    categoryEmoji = item.optString("categoryEmoji").trim().ifBlank { "✨" },
                    question = text,
                    options = options,
                    correctAnswer = correct,
                    explanation = item.optString("explanation").trim().ifBlank {
                        "الإجابة الصحيحة هي: ${options[correct]}"
                    },
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                )
            )
            existingIds.add(id)
            imported += 1
        }

        if (imported > 0) save(current)
        return imported
    }

    private fun save(questions: List<StoredQuestion>) {
        val array = JSONArray()
        questions.forEach { array.put(it.toJson()) }
        preferences.edit().putString(QUESTIONS_KEY, array.toString()).apply()
    }

    private fun StoredQuestion.toJson(): JSONObject {
        val optionsArray = JSONArray()
        options.forEach { optionsArray.put(it) }
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

@Composable
private fun IslamicQuizApp() {
    val context = LocalContext.current
    val store = remember { QuestionPackStore(context.applicationContext) }
    var customQuestions by remember { mutableStateOf(store.load()) }
    var view by remember { mutableStateOf(QuizView.HOME) }
    var selectedCategory by remember { mutableStateOf<GameCategory?>(null) }
    var activeQuestions by remember { mutableStateOf(emptyList<GameQuestion>()) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }

    val categories = remember(customQuestions) {
        mergeCategories(builtInCategories(), customCategories(customQuestions))
    }

    fun start(category: GameCategory) {
        selectedCategory = category
        activeQuestions = category.questions.shuffled()
        questionIndex = 0
        score = 0
        selectedAnswer = null
        view = QuizView.QUIZ
    }

    fun home() {
        view = QuizView.HOME
        selectedCategory = null
        activeQuestions = emptyList()
        questionIndex = 0
        score = 0
        selectedAnswer = null
    }

    BackHandler(enabled = view != QuizView.HOME) { home() }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (view) {
            QuizView.HOME -> QuizHomeScreen(
                categories = categories,
                customQuestionCount = customQuestions.size,
                onCategorySelected = ::start,
                onManage = { view = QuizView.MANAGE }
            )

            QuizView.QUIZ -> {
                val category = selectedCategory
                if (category != null && activeQuestions.isNotEmpty()) {
                    GameQuestionScreen(
                        category = category,
                        question = activeQuestions[questionIndex],
                        index = questionIndex,
                        total = activeQuestions.size,
                        score = score,
                        selectedAnswer = selectedAnswer,
                        onBack = ::home,
                        onAnswer = { answer ->
                            if (selectedAnswer == null) {
                                selectedAnswer = answer
                                if (answer == activeQuestions[questionIndex].correctAnswer) score += 1
                            }
                        },
                        onNext = {
                            if (questionIndex == activeQuestions.lastIndex) {
                                view = QuizView.RESULT
                            } else {
                                questionIndex += 1
                                selectedAnswer = null
                            }
                        }
                    )
                }
            }

            QuizView.RESULT -> {
                val category = selectedCategory
                if (category != null) {
                    GameResultScreen(
                        category = category,
                        score = score,
                        total = activeQuestions.size,
                        onReplay = { start(category) },
                        onHome = ::home
                    )
                }
            }

            QuizView.MANAGE -> QuestionManagerScreen(
                store = store,
                questions = customQuestions,
                onQuestionsChanged = { customQuestions = store.load() },
                onBack = ::home
            )
        }
    }
}

@Composable
private fun QuizHomeScreen(
    categories: List<GameCategory>,
    customQuestionCount: Int,
    onCategorySelected: (GameCategory) -> Unit,
    onManage: () -> Unit
) {
    val totalQuestions = categories.sumOf { it.questions.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("☪️", fontSize = 48.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "اختبر معلوماتك الإسلامية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "العب بالأسئلة الأساسية أو أضف أسئلتك وشاركها",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("📚", categories.size.toString(), "أقسام", Modifier.weight(1f))
                StatCard("❓", totalQuestions.toString(), "سؤالًا", Modifier.weight(1f))
                StatCard("✍️", customQuestionCount.toString(), "مضافة", Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onManage),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
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
                            if (customQuestionCount == 0) "أضف أول سؤال مخصّص" else "$customQuestionCount سؤالًا جاهزًا للمشاركة",
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("اختر نوع الأسئلة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }

        items(categories, key = { it.title }) { category ->
            CategoryGameCard(category = category, onClick = { onCategorySelected(category) })
        }
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CategoryGameCard(category: GameCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) { Text(category.emoji, fontSize = 32.sp) }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(category.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${category.questions.size} أسئلة",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("←", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameQuestionScreen(
    category: GameCategory,
    question: GameQuestion,
    index: Int,
    total: Int,
    score: Int,
    selectedAnswer: Int?,
    onBack: () -> Unit,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    val answered = selectedAnswer != null
    val isCorrect = selectedAnswer == question.correctAnswer

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
                    Text(category.title, fontWeight = FontWeight.Black)
                    Text(
                        "السؤال ${index + 1} من $total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text("⭐ $score", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Black)
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
                LinearProgressIndicator(
                    progress = { (index + 1).toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(category.emoji, fontSize = 44.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            question.question,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(question.options.indices.toList()) { optionIndex ->
                GameAnswerButton(
                    text = question.options[optionIndex],
                    optionIndex = optionIndex,
                    correctAnswer = question.correctAnswer,
                    selectedAnswer = selectedAnswer,
                    onClick = { onAnswer(optionIndex) }
                )
            }

            if (answered) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isCorrect) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isCorrect) "إجابة صحيحة!" else "الإجابة الصحيحة: ${question.options[question.correctAnswer]}",
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(question.explanation)
                        }
                    }
                }

                item {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(if (index == total - 1) "عرض النتيجة" else "السؤال التالي", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameAnswerButton(
    text: String,
    optionIndex: Int,
    correctAnswer: Int,
    selectedAnswer: Int?,
    onClick: () -> Unit
) {
    val answered = selectedAnswer != null
    val containerColor = when {
        answered && optionIndex == correctAnswer -> MaterialTheme.colorScheme.secondaryContainer
        answered && optionIndex == selectedAnswer -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }

    OutlinedButton(
        onClick = onClick,
        enabled = !answered,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GameResultScreen(
    category: GameCategory,
    score: Int,
    total: Int,
    onReplay: () -> Unit,
    onHome: () -> Unit
) {
    val percentage = if (total == 0) 0 else score * 100 / total
    val message = when {
        percentage == 100 -> "ما شاء الله! إجابات كاملة"
        percentage >= 80 -> "ممتاز جدًا! معلوماتك قوية"
        percentage >= 60 -> "جيد جدًا، استمر في التعلم"
        percentage >= 40 -> "بداية جيدة، جرّب مرة أخرى"
        else -> "لا بأس، التعلم بالمحاولة"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, modifier = Modifier.size(68.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("انتهى قسم ${category.title}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("$score / $total", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("$percentage٪", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onReplay, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("إعادة اللعب", fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Rounded.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("اختيار قسم آخر", fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionManagerScreen(
    store: QuestionPackStore,
    questions: List<StoredQuestion>,
    onQuestionsChanged: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "العودة") }
                Column(modifier = Modifier.weight(1f)) {
                    Text("إنشاء وإرسال الأسئلة", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("الأسئلة المخصّصة محفوظة على هاتفك", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("${questions.size} سؤالًا مخصّصًا", fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("${questions.map { it.categoryTitle }.distinct().size} أقسام مخصّصة")
                    }
                }
            }

            item {
                Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إضافة سؤال جديد", fontWeight = FontWeight.Black)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            if (questions.isEmpty()) message = "أضف سؤالًا واحدًا على الأقل قبل المشاركة."
                            else sharePack(context, store.exportPack())
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("مشاركة")
                    }
                    OutlinedButton(
                        onClick = { showImport = true },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("استيراد")
                    }
                }
            }

            message?.let { value ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Text(value, modifier = Modifier.fillMaxWidth().padding(14.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (questions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✍️", fontSize = 52.sp)
                        Text("لم تضف أسئلة بعد", fontWeight = FontWeight.Black)
                        Text("أضف أي عدد من الأسئلة ثم شاركها مع المستخدمين", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(questions.sortedByDescending { it.createdAt }, key = { it.id }) { question ->
                    StoredQuestionCard(
                        question = question,
                        onDelete = {
                            store.delete(question.id)
                            onQuestionsChanged()
                            message = "تم حذف السؤال."
                        }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddQuestionDialog(
            onDismiss = { showAdd = false },
            onSave = { category, emoji, question, options, correct, explanation ->
                if (store.add(category, emoji, question, options, correct, explanation)) {
                    onQuestionsChanged()
                    showAdd = false
                    message = "تمت إضافة السؤال وأصبح جاهزًا للعب والمشاركة."
                }
            }
        )
    }

    if (showImport) {
        ImportPackDialog(
            onDismiss = { showImport = false },
            onImport = { text ->
                runCatching { store.importPack(text) }
                    .onSuccess { count ->
                        onQuestionsChanged()
                        showImport = false
                        message = if (count > 0) "تم استيراد $count سؤالًا بنجاح." else "لا توجد أسئلة جديدة في الحزمة."
                    }
                    .onFailure { error -> message = error.message ?: "تعذر استيراد الحزمة." }
            }
        )
    }
}

@Composable
private fun StoredQuestionCard(question: StoredQuestion, onDelete: () -> Unit) {
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
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("الإجابة: ${question.options[question.correctAnswer]}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddQuestionDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<String>, Int, String) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    var question by remember { mutableStateOf("") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }
    var option4 by remember { mutableStateOf("") }
    var correct by remember { mutableIntStateOf(0) }
    var explanation by remember { mutableStateOf("") }
    val options = listOf(option1, option2, option3, option4)
    val valid = category.isNotBlank() && question.isNotBlank() && options.all { it.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة سؤال جديد", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it.take(3) }, modifier = Modifier.weight(0.3f), label = { Text("رمز") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, modifier = Modifier.weight(0.7f), label = { Text("اسم القسم") }, singleLine = true)
                }
                OutlinedTextField(value = question, onValueChange = { question = it }, modifier = Modifier.fillMaxWidth(), label = { Text("السؤال") }, minLines = 2)
                OutlinedTextField(value = option1, onValueChange = { option1 = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاختيار الأول") })
                OutlinedTextField(value = option2, onValueChange = { option2 = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاختيار الثاني") })
                OutlinedTextField(value = option3, onValueChange = { option3 = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاختيار الثالث") })
                OutlinedTextField(value = option4, onValueChange = { option4 = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاختيار الرابع") })
                Text("الإجابة الصحيحة", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { index ->
                        FilterChip(selected = correct == index, onClick = { correct = index }, label = { Text("${index + 1}") }, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(value = explanation, onValueChange = { explanation = it }, modifier = Modifier.fillMaxWidth(), label = { Text("شرح الإجابة — اختياري") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(category, emoji, question, options, correct, explanation) }) {
                Text("حفظ السؤال")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun ImportPackDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("استيراد حزمة أسئلة", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("انسخ رسالة الحزمة التي وصلتك والصقها هنا كاملة.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().height(220.dp), label = { Text("نص الحزمة") })
            }
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onImport(text) }) { Text("استيراد") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private fun sharePack(context: Context, json: String) {
    val message = buildString {
        appendLine("حزمة أسئلة للعبة اختبر معلوماتك الإسلامية")
        appendLine("انسخ الرسالة كاملة ثم افتح اللعبة واختر: إنشاء وإرسال أسئلتك ← استيراد.")
        appendLine()
        append(json)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "حزمة أسئلة إسلامية")
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(intent, "إرسال حزمة الأسئلة"))
}

private fun customCategories(questions: List<StoredQuestion>): List<GameCategory> =
    questions.groupBy { it.categoryTitle }.map { (title, list) ->
        GameCategory(
            title = title,
            emoji = list.firstOrNull()?.categoryEmoji ?: "✨",
            description = "أسئلة مخصّصة أضافها المستخدمون",
            questions = list.map {
                GameQuestion(it.question, it.options, it.correctAnswer, it.explanation)
            }
        )
    }

private fun mergeCategories(base: List<GameCategory>, custom: List<GameCategory>): List<GameCategory> =
    (base + custom)
        .groupBy { it.title }
        .map { (title, grouped) ->
            val first = grouped.first()
            GameCategory(
                title = title,
                emoji = first.emoji,
                description = if (grouped.size > 1) "الأسئلة الأساسية والأسئلة المضافة" else first.description,
                questions = grouped.flatMap { it.questions }
            )
        }

private fun builtInCategories(): List<GameCategory> = listOf(
    GameCategory(
        "القرآن الكريم", "📖", "السور وأجزاء القرآن ومعلومات أساسية",
        listOf(
            GameQuestion("ما أول سورة في ترتيب المصحف؟", listOf("البقرة", "الفاتحة", "الناس", "الإخلاص"), 1, "سورة الفاتحة هي السورة الأولى في ترتيب المصحف."),
            GameQuestion("ما أطول سورة في القرآن الكريم؟", listOf("آل عمران", "النساء", "البقرة", "المائدة"), 2, "سورة البقرة هي أطول سور القرآن الكريم."),
            GameQuestion("كم عدد أجزاء القرآن الكريم؟", listOf("20", "25", "30", "40"), 2, "القرآن الكريم مقسّم إلى ثلاثين جزءًا."),
            GameQuestion("ما السورة التي لا تبدأ بالبسملة؟", listOf("الأنفال", "التوبة", "يونس", "هود"), 1, "سورة التوبة هي السورة الوحيدة التي لا تبدأ بالبسملة."),
            GameQuestion("في أي سورة وردت البسملة مرتين؟", listOf("النمل", "مريم", "يس", "الرحمن"), 0, "وردت البسملة في بداية سورة النمل وفي رسالة سليمان عليه السلام.")
        )
    ),
    GameCategory(
        "الأنبياء", "🌟", "قصص الأنبياء وألقابهم ومواقفهم",
        listOf(
            GameQuestion("من أول الأنبياء؟", listOf("نوح", "إبراهيم", "آدم", "موسى"), 2, "آدم عليه السلام هو أول البشر وأول الأنبياء."),
            GameQuestion("من النبي الذي ابتلعه الحوت؟", listOf("يونس", "يوسف", "أيوب", "هود"), 0, "ابتلع الحوت نبي الله يونس عليه السلام."),
            GameQuestion("من النبي الذي صنع السفينة بأمر الله؟", listOf("صالح", "نوح", "لوط", "شعيب"), 1, "أمر الله نوحًا عليه السلام أن يصنع السفينة."),
            GameQuestion("من النبي الملقب بكليم الله؟", listOf("عيسى", "داود", "موسى", "إسماعيل"), 2, "موسى عليه السلام هو كليم الله."),
            GameQuestion("من النبي المعروف بأبي الأنبياء؟", listOf("إبراهيم", "يعقوب", "زكريا", "إدريس"), 0, "إبراهيم عليه السلام يُعرف بأبي الأنبياء.")
        )
    ),
    GameCategory(
        "السيرة النبوية", "🕌", "حياة النبي ﷺ والهجرة وبداية الوحي",
        listOf(
            GameQuestion("أين وُلد النبي محمد ﷺ؟", listOf("المدينة", "مكة", "الطائف", "القدس"), 1, "وُلد النبي محمد ﷺ في مكة المكرمة."),
            GameQuestion("في أي غار نزل الوحي أول مرة؟", listOf("غار ثور", "غار حراء", "غار أحد", "غار بدر"), 1, "نزل الوحي أول مرة في غار حراء."),
            GameQuestion("إلى أي مدينة هاجر النبي ﷺ؟", listOf("الطائف", "القدس", "المدينة المنورة", "دمشق"), 2, "هاجر النبي ﷺ من مكة إلى المدينة المنورة."),
            GameQuestion("من أول زوجات النبي ﷺ؟", listOf("عائشة", "حفصة", "خديجة", "سودة"), 2, "خديجة بنت خويلد رضي الله عنها هي أول زوجاته ﷺ."),
            GameQuestion("ما اسم العام الذي توفيت فيه خديجة وأبو طالب؟", listOf("عام الفيل", "عام الحزن", "عام الوفود", "عام الرمادة"), 1, "سُمّي عام الحزن لوفاة خديجة وأبي طالب.")
        )
    ),
    GameCategory(
        "العبادات", "🤲", "الصلاة والصيام والزكاة والحج",
        listOf(
            GameQuestion("كم عدد الصلوات المفروضة في اليوم؟", listOf("ثلاث", "أربع", "خمس", "ست"), 2, "فرض الله خمس صلوات في اليوم والليلة."),
            GameQuestion("في أي شهر يصوم المسلمون؟", listOf("شعبان", "رمضان", "شوال", "محرم"), 1, "يصوم المسلمون شهر رمضان."),
            GameQuestion("إلى أي جهة يتجه المسلم في الصلاة؟", listOf("المسجد النبوي", "المسجد الأقصى", "الكعبة", "جبل عرفات"), 2, "قبلة المسلمين هي الكعبة المشرفة."),
            GameQuestion("كم عدد ركعات صلاة الفجر؟", listOf("ركعتان", "ثلاث", "أربع", "خمس"), 0, "صلاة الفجر المفروضة ركعتان."),
            GameQuestion("ما العبادة التي تؤدى في مكة في وقت مخصوص؟", listOf("الزكاة", "الحج", "الاعتكاف", "الأضحية"), 1, "الحج عبادة تؤدى في مكة والمشاعر المقدسة في وقت مخصوص.")
        )
    ),
    GameCategory(
        "الصحابة", "🏅", "الخلفاء والصحابة ومواقفهم",
        listOf(
            GameQuestion("من أول الخلفاء الراشدين؟", listOf("عمر بن الخطاب", "أبو بكر الصديق", "عثمان بن عفان", "علي بن أبي طالب"), 1, "أبو بكر الصديق رضي الله عنه هو أول الخلفاء الراشدين."),
            GameQuestion("من الملقب بالفاروق؟", listOf("عمر بن الخطاب", "خالد بن الوليد", "أبو عبيدة", "سعد بن أبي وقاص"), 0, "عمر بن الخطاب رضي الله عنه هو الفاروق."),
            GameQuestion("من الملقب بذي النورين؟", listOf("علي بن أبي طالب", "عثمان بن عفان", "طلحة بن عبيد الله", "الزبير بن العوام"), 1, "عثمان بن عفان رضي الله عنه لُقب بذي النورين."),
            GameQuestion("من الملقب بسيف الله المسلول؟", listOf("خالد بن الوليد", "حمزة بن عبد المطلب", "أبو دجانة", "بلال بن رباح"), 0, "خالد بن الوليد رضي الله عنه هو سيف الله المسلول."),
            GameQuestion("من كان مؤذن الرسول ﷺ؟", listOf("سلمان الفارسي", "بلال بن رباح", "أبو ذر الغفاري", "صهيب الرومي"), 1, "بلال بن رباح رضي الله عنه كان مؤذن الرسول ﷺ.")
        )
    )
)
