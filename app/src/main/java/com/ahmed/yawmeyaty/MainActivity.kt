package com.ahmed.yawmeyaty

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
import com.ahmed.yawmeyaty.ui.theme.YawmeyatyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YawmeyatyTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    IslamicQuizGame()
                }
            }
        }
    }
}

private enum class GameScreen {
    HOME,
    QUIZ,
    RESULT
}

private data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

private data class QuizCategory(
    val title: String,
    val emoji: String,
    val description: String,
    val questions: List<QuizQuestion>
)

@Composable
private fun IslamicQuizGame() {
    var screen by remember { mutableStateOf(GameScreen.HOME) }
    var selectedCategory by remember { mutableStateOf<QuizCategory?>(null) }
    var questions by remember { mutableStateOf(emptyList<QuizQuestion>()) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }

    fun startCategory(category: QuizCategory) {
        selectedCategory = category
        questions = category.questions.shuffled()
        questionIndex = 0
        score = 0
        selectedAnswer = null
        screen = GameScreen.QUIZ
    }

    fun returnHome() {
        screen = GameScreen.HOME
        selectedCategory = null
        questions = emptyList()
        questionIndex = 0
        score = 0
        selectedAnswer = null
    }

    BackHandler(enabled = screen != GameScreen.HOME) {
        returnHome()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            GameScreen.HOME -> HomeScreen(onCategorySelected = ::startCategory)

            GameScreen.QUIZ -> {
                val category = selectedCategory
                if (category != null && questions.isNotEmpty()) {
                    QuizScreen(
                        category = category,
                        question = questions[questionIndex],
                        questionIndex = questionIndex,
                        totalQuestions = questions.size,
                        score = score,
                        selectedAnswer = selectedAnswer,
                        onBack = ::returnHome,
                        onAnswer = { answerIndex ->
                            if (selectedAnswer == null) {
                                selectedAnswer = answerIndex
                                if (answerIndex == questions[questionIndex].correctAnswer) {
                                    score += 1
                                }
                            }
                        },
                        onNext = {
                            if (questionIndex == questions.lastIndex) {
                                screen = GameScreen.RESULT
                            } else {
                                questionIndex += 1
                                selectedAnswer = null
                            }
                        }
                    )
                }
            }

            GameScreen.RESULT -> {
                val category = selectedCategory
                if (category != null) {
                    ResultScreen(
                        category = category,
                        score = score,
                        totalQuestions = questions.size,
                        onReplay = { startCategory(category) },
                        onHome = ::returnHome
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onCategorySelected: (QuizCategory) -> Unit) {
    val categories = remember { quizCategories() }

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
                    text = "اختبر معلوماتك الإسلامية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "اختر قسمًا وأجب عن 5 أسئلة",
                    style = MaterialTheme.typography.bodyLarge,
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
                MiniStatCard(
                    emoji = "📚",
                    value = "5",
                    label = "أقسام",
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    emoji = "❓",
                    value = "25",
                    label = "سؤالًا",
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    emoji = "🏆",
                    value = "5",
                    label = "نقاط لكل قسم",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "اختر نوع الأسئلة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(categories, key = { it.title }) { category ->
            CategoryCard(category = category, onClick = { onCategorySelected(category) })
        }
    }
}

@Composable
private fun MiniStatCard(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
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
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun CategoryCard(category: QuizCategory, onClick: () -> Unit) {
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
                Box(contentAlignment = Alignment.Center) {
                    Text(category.emoji, fontSize = 32.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "5 أسئلة",
                    style = MaterialTheme.typography.labelLarge,
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
private fun QuizScreen(
    category: QuizCategory,
    question: QuizQuestion,
    questionIndex: Int,
    totalQuestions: Int,
    score: Int,
    selectedAnswer: Int?,
    onBack: () -> Unit,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    val answered = selectedAnswer != null
    val isCorrect = selectedAnswer == question.correctAnswer
    val progress = (questionIndex + 1).toFloat() / totalQuestions.toFloat()

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
                        "السؤال ${questionIndex + 1} من $totalQuestions",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "⭐ $score",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Black
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
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
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
                            text = question.question,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(question.options.indices.toList()) { optionIndex ->
                AnswerButton(
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
                            containerColor = if (isCorrect) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isCorrect) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isCorrect) "إجابة صحيحة!" else "الإجابة الصحيحة: ${question.options[question.correctAnswer]}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                item {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = if (questionIndex == totalQuestions - 1) "عرض النتيجة" else "السؤال التالي",
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerButton(
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
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResultScreen(
    category: QuizCategory,
    score: Int,
    totalQuestions: Int,
    onReplay: () -> Unit,
    onHome: () -> Unit
) {
    val percentage = if (totalQuestions == 0) 0 else score * 100 / totalQuestions
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
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
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
            text = "انتهى قسم ${category.title}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$score / $totalQuestions",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$percentage٪",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onReplay,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("إعادة اللعب", fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Rounded.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("اختيار قسم آخر", fontWeight = FontWeight.Black)
        }
    }
}

private fun quizCategories(): List<QuizCategory> = listOf(
    QuizCategory(
        title = "القرآن الكريم",
        emoji = "📖",
        description = "السور وأجزاء القرآن ومعلومات أساسية",
        questions = listOf(
            QuizQuestion(
                question = "ما أول سورة في ترتيب المصحف؟",
                options = listOf("سورة البقرة", "سورة الفاتحة", "سورة الناس", "سورة الإخلاص"),
                correctAnswer = 1,
                explanation = "سورة الفاتحة هي السورة الأولى في ترتيب المصحف الشريف."
            ),
            QuizQuestion(
                question = "ما أطول سورة في القرآن الكريم؟",
                options = listOf("سورة آل عمران", "سورة النساء", "سورة البقرة", "سورة المائدة"),
                correctAnswer = 2,
                explanation = "سورة البقرة هي أطول سور القرآن الكريم، وعدد آياتها 286 آية."
            ),
            QuizQuestion(
                question = "كم عدد أجزاء القرآن الكريم؟",
                options = listOf("20 جزءًا", "25 جزءًا", "30 جزءًا", "40 جزءًا"),
                correctAnswer = 2,
                explanation = "القرآن الكريم مقسّم إلى ثلاثين جزءًا."
            ),
            QuizQuestion(
                question = "ما السورة التي لا تبدأ بالبسملة؟",
                options = listOf("سورة الأنفال", "سورة التوبة", "سورة يونس", "سورة هود"),
                correctAnswer = 1,
                explanation = "سورة التوبة هي السورة الوحيدة التي لا تبدأ ببسم الله الرحمن الرحيم."
            ),
            QuizQuestion(
                question = "في أي سورة وردت البسملة مرتين؟",
                options = listOf("سورة النمل", "سورة مريم", "سورة يس", "سورة الرحمن"),
                correctAnswer = 0,
                explanation = "وردت البسملة في بداية سورة النمل، ووردت مرة أخرى في رسالة سليمان عليه السلام."
            )
        )
    ),
    QuizCategory(
        title = "الأنبياء",
        emoji = "🌟",
        description = "قصص الأنبياء وألقابهم ومواقفهم",
        questions = listOf(
            QuizQuestion(
                question = "من أول الأنبياء؟",
                options = listOf("نوح عليه السلام", "إبراهيم عليه السلام", "آدم عليه السلام", "موسى عليه السلام"),
                correctAnswer = 2,
                explanation = "آدم عليه السلام هو أول البشر وأول الأنبياء."
            ),
            QuizQuestion(
                question = "من النبي الذي ابتلعه الحوت؟",
                options = listOf("يونس عليه السلام", "يوسف عليه السلام", "أيوب عليه السلام", "هود عليه السلام"),
                correctAnswer = 0,
                explanation = "ابتلع الحوت نبي الله يونس عليه السلام، فدعا الله في الظلمات فنجّاه."
            ),
            QuizQuestion(
                question = "من النبي الذي صنع السفينة بأمر الله؟",
                options = listOf("صالح عليه السلام", "نوح عليه السلام", "لوط عليه السلام", "شعيب عليه السلام"),
                correctAnswer = 1,
                explanation = "أمر الله نوحًا عليه السلام أن يصنع السفينة لينجو المؤمنون من الطوفان."
            ),
            QuizQuestion(
                question = "من النبي الملقب بكليم الله؟",
                options = listOf("عيسى عليه السلام", "داود عليه السلام", "موسى عليه السلام", "إسماعيل عليه السلام"),
                correctAnswer = 2,
                explanation = "موسى عليه السلام هو كليم الله؛ لأن الله كلّمه تكليمًا."
            ),
            QuizQuestion(
                question = "من النبي المعروف بأبي الأنبياء؟",
                options = listOf("إبراهيم عليه السلام", "يعقوب عليه السلام", "زكريا عليه السلام", "إدريس عليه السلام"),
                correctAnswer = 0,
                explanation = "إبراهيم عليه السلام يُعرف بأبي الأنبياء؛ لأن كثيرًا من الأنبياء جاءوا من ذريته."
            )
        )
    ),
    QuizCategory(
        title = "السيرة النبوية",
        emoji = "🕌",
        description = "حياة النبي ﷺ والهجرة وبداية الوحي",
        questions = listOf(
            QuizQuestion(
                question = "أين وُلد النبي محمد ﷺ؟",
                options = listOf("المدينة المنورة", "مكة المكرمة", "الطائف", "القدس"),
                correctAnswer = 1,
                explanation = "وُلد النبي محمد ﷺ في مكة المكرمة."
            ),
            QuizQuestion(
                question = "إلى أي مدينة هاجر النبي ﷺ؟",
                options = listOf("الطائف", "دمشق", "المدينة المنورة", "بيت المقدس"),
                correctAnswer = 2,
                explanation = "هاجر النبي ﷺ من مكة إلى المدينة المنورة."
            ),
            QuizQuestion(
                question = "ما اسم الغار الذي اختبأ فيه النبي ﷺ أثناء الهجرة؟",
                options = listOf("غار حراء", "غار ثور", "غار أحد", "غار بدر"),
                correctAnswer = 1,
                explanation = "مكث النبي ﷺ وأبو بكر رضي الله عنه في غار ثور أثناء الهجرة."
            ),
            QuizQuestion(
                question = "من أول زوجات النبي ﷺ؟",
                options = listOf("عائشة رضي الله عنها", "حفصة رضي الله عنها", "خديجة رضي الله عنها", "أم سلمة رضي الله عنها"),
                correctAnswer = 2,
                explanation = "خديجة بنت خويلد رضي الله عنها هي أول زوجات النبي ﷺ وأول من آمن به."
            ),
            QuizQuestion(
                question = "كم كان عمر النبي ﷺ عندما بدأ نزول الوحي؟",
                options = listOf("30 عامًا", "35 عامًا", "40 عامًا", "45 عامًا"),
                correctAnswer = 2,
                explanation = "بدأ نزول الوحي على النبي ﷺ عندما بلغ أربعين عامًا."
            )
        )
    ),
    QuizCategory(
        title = "العبادات",
        emoji = "🤲",
        description = "أركان الإسلام والصلاة والصيام والحج",
        questions = listOf(
            QuizQuestion(
                question = "كم عدد أركان الإسلام؟",
                options = listOf("أربعة", "خمسة", "ستة", "سبعة"),
                correctAnswer = 1,
                explanation = "أركان الإسلام خمسة: الشهادتان، والصلاة، والزكاة، والصوم، والحج."
            ),
            QuizQuestion(
                question = "كم عدد الصلوات المفروضة في اليوم والليلة؟",
                options = listOf("ثلاث صلوات", "أربع صلوات", "خمس صلوات", "ست صلوات"),
                correctAnswer = 2,
                explanation = "فرض الله على المسلمين خمس صلوات في اليوم والليلة."
            ),
            QuizQuestion(
                question = "في أي شهر يصوم المسلمون؟",
                options = listOf("شعبان", "رمضان", "شوال", "محرم"),
                correctAnswer = 1,
                explanation = "صيام شهر رمضان هو الركن الرابع من أركان الإسلام."
            ),
            QuizQuestion(
                question = "إلى أي جهة يتجه المسلمون في الصلاة؟",
                options = listOf("المسجد النبوي", "المسجد الأقصى", "الكعبة المشرفة", "جبل عرفات"),
                correctAnswer = 2,
                explanation = "يتجه المسلمون في الصلاة إلى الكعبة المشرفة في المسجد الحرام."
            ),
            QuizQuestion(
                question = "في أي شهر هجري تؤدى مناسك الحج؟",
                options = listOf("رمضان", "شوال", "ذو القعدة", "ذو الحجة"),
                correctAnswer = 3,
                explanation = "تؤدى مناسك الحج في شهر ذي الحجة."
            )
        )
    ),
    QuizCategory(
        title = "الصحابة",
        emoji = "🏅",
        description = "الخلفاء الراشدون وأشهر الصحابة",
        questions = listOf(
            QuizQuestion(
                question = "من أول الخلفاء الراشدين؟",
                options = listOf("عمر بن الخطاب", "أبو بكر الصديق", "عثمان بن عفان", "علي بن أبي طالب"),
                correctAnswer = 1,
                explanation = "أبو بكر الصديق رضي الله عنه هو أول الخلفاء الراشدين."
            ),
            QuizQuestion(
                question = "من ثاني الخلفاء الراشدين؟",
                options = listOf("عمر بن الخطاب", "عثمان بن عفان", "علي بن أبي طالب", "أبو عبيدة بن الجراح"),
                correctAnswer = 0,
                explanation = "عمر بن الخطاب رضي الله عنه هو ثاني الخلفاء الراشدين."
            ),
            QuizQuestion(
                question = "من الصحابي الملقب بذي النورين؟",
                options = listOf("طلحة بن عبيد الله", "الزبير بن العوام", "عثمان بن عفان", "سعد بن أبي وقاص"),
                correctAnswer = 2,
                explanation = "لُقّب عثمان بن عفان رضي الله عنه بذي النورين لزواجه من ابنتين للنبي ﷺ."
            ),
            QuizQuestion(
                question = "من رابع الخلفاء الراشدين؟",
                options = listOf("علي بن أبي طالب", "معاوية بن أبي سفيان", "خالد بن الوليد", "عبد الله بن مسعود"),
                correctAnswer = 0,
                explanation = "علي بن أبي طالب رضي الله عنه هو رابع الخلفاء الراشدين."
            ),
            QuizQuestion(
                question = "من أشهر مؤذني النبي ﷺ؟",
                options = listOf("بلال بن رباح", "زيد بن ثابت", "سلمان الفارسي", "أبو هريرة"),
                correctAnswer = 0,
                explanation = "بلال بن رباح رضي الله عنه من أشهر مؤذني النبي ﷺ."
            )
        )
    )
)
