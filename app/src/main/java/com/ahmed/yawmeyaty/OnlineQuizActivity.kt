package com.ahmed.yawmeyaty

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ahmed.yawmeyaty.ui.theme.YawmeyatyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date

class OnlineQuizActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YawmeyatyTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    OnlineIslamicQuiz()
                }
            }
        }
    }
}

private enum class OnlineQuizScreen {
    HOME,
    QUIZ,
    RESULT
}

private data class OnlineQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

private data class OnlineCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val description: String,
    val questions: List<OnlineQuestion>
)

private data class OnlineQuestionBank(
    val version: Int,
    val updatedAt: String,
    val categories: List<OnlineCategory>
)

private data class SyncResult(
    val bank: OnlineQuestionBank,
    val downloaded: Boolean,
    val message: String
)

private class OnlineQuestionBankRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): OnlineQuestionBank {
        val cached = preferences.getString(CACHED_BANK_KEY, null)
        if (!cached.isNullOrBlank()) {
            parseBank(cached)?.let { return it }
        }

        val bundled = context.assets.open(ASSET_FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return requireNotNull(parseBank(bundled)) { "Bundled question bank is invalid." }
    }

    fun lastSyncLabel(): String {
        val timestamp = preferences.getLong(LAST_SYNC_KEY, 0L)
        if (timestamp <= 0L) return "لم تتم المزامنة بعد"
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
    }

    suspend fun sync(): SyncResult = withContext(Dispatchers.IO) {
        val current = load()

        runCatching {
            val connection = (URL("$REMOTE_URL?t=${System.currentTimeMillis()}").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "IslamicQuizAndroid/3.2")
            }

            try {
                val statusCode = connection.responseCode
                require(statusCode in 200..299) { "HTTP $statusCode" }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val remote = requireNotNull(parseBank(body)) { "ملف الأسئلة غير صالح" }
                require(remote.categories.isNotEmpty()) { "بنك الأسئلة فارغ" }

                preferences.edit()
                    .putString(CACHED_BANK_KEY, body)
                    .putLong(LAST_SYNC_KEY, System.currentTimeMillis())
                    .apply()

                val changed = remote.version != current.version || remote != current
                SyncResult(
                    bank = remote,
                    downloaded = changed,
                    message = if (changed) {
                        "تم تحميل الإصدار ${remote.version} بنجاح"
                    } else {
                        "لديك أحدث إصدار من الأسئلة"
                    }
                )
            } finally {
                connection.disconnect()
            }
        }.getOrElse {
            SyncResult(
                bank = current,
                downloaded = false,
                message = "تعذر الاتصال. يتم استخدام آخر نسخة محفوظة."
            )
        }
    }

    private fun parseBank(raw: String): OnlineQuestionBank? = runCatching {
        val root = JSONObject(raw)
        require(root.optString("format") == BANK_FORMAT)
        val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
        val categories = buildList {
            for (categoryIndex in 0 until categoriesArray.length()) {
                val categoryObject = categoriesArray.optJSONObject(categoryIndex) ?: continue
                val questionsArray = categoryObject.optJSONArray("questions") ?: continue
                val questions = buildList {
                    for (questionIndex in 0 until questionsArray.length()) {
                        val questionObject = questionsArray.optJSONObject(questionIndex) ?: continue
                        val optionsArray = questionObject.optJSONArray("options") ?: continue
                        if (optionsArray.length() != 4) continue

                        val options = List(4) { optionsArray.optString(it).trim() }
                        val questionText = questionObject.optString("question").trim()
                        val correctAnswer = questionObject.optInt("correctAnswer", -1)
                        if (questionText.isBlank() || options.any { it.isBlank() } || correctAnswer !in 0..3) continue

                        add(
                            OnlineQuestion(
                                id = questionObject.optString("id").ifBlank { "q-$categoryIndex-$questionIndex" },
                                question = questionText,
                                options = options,
                                correctAnswer = correctAnswer,
                                explanation = questionObject.optString("explanation").trim().ifBlank {
                                    "الإجابة الصحيحة هي: ${options[correctAnswer]}"
                                }
                            )
                        )
                    }
                }

                if (questions.isNotEmpty()) {
                    add(
                        OnlineCategory(
                            id = categoryObject.optString("id").ifBlank { "category-$categoryIndex" },
                            title = categoryObject.optString("title").trim().ifBlank { "قسم جديد" },
                            emoji = categoryObject.optString("emoji").trim().ifBlank { "✨" },
                            description = categoryObject.optString("description").trim().ifBlank { "أسئلة متنوعة" },
                            questions = questions
                        )
                    )
                }
            }
        }

        OnlineQuestionBank(
            version = root.optInt("version", 1),
            updatedAt = root.optString("updatedAt").ifBlank { "غير محدد" },
            categories = categories
        )
    }.getOrNull()

    companion object {
        private const val PREFS_NAME = "online_islamic_quiz"
        private const val CACHED_BANK_KEY = "cached_question_bank"
        private const val LAST_SYNC_KEY = "last_sync"
        private const val ASSET_FILE = "questions.json"
        private const val BANK_FORMAT = "islamic_quiz_question_bank"
        private const val REMOTE_URL = "https://raw.githubusercontent.com/ahmed15500/New/main/question-bank/questions.json"
    }
}

@Composable
private fun OnlineIslamicQuiz() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { OnlineQuestionBankRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var bank by remember { mutableStateOf(repository.load()) }
    var syncMessage by remember { mutableStateOf("يتم فحص تحديثات الأسئلة...") }
    var lastSync by remember { mutableStateOf(repository.lastSyncLabel()) }
    var syncing by remember { mutableStateOf(false) }

    var screen by remember { mutableStateOf(OnlineQuizScreen.HOME) }
    var selectedCategory by remember { mutableStateOf<OnlineCategory?>(null) }
    var questions by remember { mutableStateOf(emptyList<OnlineQuestion>()) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }

    fun syncQuestions() {
        if (syncing) return
        syncing = true
        scope.launch {
            val result = repository.sync()
            bank = result.bank
            syncMessage = result.message
            lastSync = repository.lastSyncLabel()
            syncing = false
        }
    }

    fun startCategory(category: OnlineCategory) {
        selectedCategory = category
        questions = category.questions.shuffled()
        questionIndex = 0
        score = 0
        selectedAnswer = null
        screen = OnlineQuizScreen.QUIZ
    }

    fun goHome() {
        screen = OnlineQuizScreen.HOME
        selectedCategory = null
        questions = emptyList()
        questionIndex = 0
        score = 0
        selectedAnswer = null
    }

    LaunchedEffect(Unit) {
        syncQuestions()
    }

    BackHandler(enabled = screen != OnlineQuizScreen.HOME) { goHome() }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            OnlineQuizScreen.HOME -> OnlineQuizHome(
                bank = bank,
                syncing = syncing,
                syncMessage = syncMessage,
                lastSync = lastSync,
                onSync = ::syncQuestions,
                onCategorySelected = ::startCategory
            )

            OnlineQuizScreen.QUIZ -> {
                val category = selectedCategory
                if (category != null && questions.isNotEmpty()) {
                    OnlineQuestionScreen(
                        category = category,
                        question = questions[questionIndex],
                        index = questionIndex,
                        total = questions.size,
                        score = score,
                        selectedAnswer = selectedAnswer,
                        onBack = ::goHome,
                        onAnswer = { answer ->
                            if (selectedAnswer == null) {
                                selectedAnswer = answer
                                if (answer == questions[questionIndex].correctAnswer) score += 1
                            }
                        },
                        onNext = {
                            if (questionIndex == questions.lastIndex) {
                                screen = OnlineQuizScreen.RESULT
                            } else {
                                questionIndex += 1
                                selectedAnswer = null
                            }
                        }
                    )
                }
            }

            OnlineQuizScreen.RESULT -> {
                val category = selectedCategory
                if (category != null) {
                    OnlineResultScreen(
                        category = category,
                        score = score,
                        total = questions.size,
                        onReplay = { startCategory(category) },
                        onHome = ::goHome
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineQuizHome(
    bank: OnlineQuestionBank,
    syncing: Boolean,
    syncMessage: String,
    lastSync: String,
    onSync: () -> Unit,
    onCategorySelected: (OnlineCategory) -> Unit
) {
    val totalQuestions = bank.categories.sumOf { it.questions.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(modifier = Modifier.size(88.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) { Text("☪️", fontSize = 48.sp) }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "اختبر معلوماتك الإسلامية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "الأسئلة تتحدث تلقائيًا عند توفر الإنترنت",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OnlineStatCard("📚", bank.categories.size.toString(), "أقسام", Modifier.weight(1f))
                OnlineStatCard("❓", totalQuestions.toString(), "سؤالًا", Modifier.weight(1f))
                OnlineStatCard("🔄", bank.version.toString(), "الإصدار", Modifier.weight(1f))
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تحديث بنك الأسئلة", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                    Text(syncMessage, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        "نسخة البنك: ${bank.updatedAt} • آخر فحص: $lastSync",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Button(
                        onClick = onSync,
                        enabled = !syncing,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (syncing) "جاري التحديث..." else "تحديث الأسئلة الآن", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Text("اختر نوع الأسئلة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }

        items(bank.categories, key = { it.id }) { category ->
            OnlineCategoryCard(category = category, onClick = { onCategorySelected(category) })
        }
    }
}

@Composable
private fun OnlineStatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
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
private fun OnlineCategoryCard(category: OnlineCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun OnlineQuestionScreen(
    category: OnlineCategory,
    question: OnlineQuestion,
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
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "العودة") }
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
                OnlineAnswerButton(
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
private fun OnlineAnswerButton(
    text: String,
    optionIndex: Int,
    correctAnswer: Int,
    selectedAnswer: Int?,
    onClick: () -> Unit
) {
    val answered = selectedAnswer != null
    val containerColor = when {
        answered && optionIndex == correctAnswer -> MaterialTheme.colorScheme.secondaryContainer
        answered && optionIndex == selectedAnswer && optionIndex != correctAnswer -> MaterialTheme.colorScheme.errorContainer
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
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OnlineResultScreen(
    category: OnlineCategory,
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
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(68.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "انتهى قسم ${category.title}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
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
