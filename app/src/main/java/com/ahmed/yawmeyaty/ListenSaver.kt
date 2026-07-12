package com.ahmed.yawmeyaty

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.UUID

private data class SavedYouTubeVideo(
    val id: String,
    val title: String,
    val url: String,
    val durationMinutes: Int,
    val targetMegabytes: Double,
    val createdAt: Long = System.currentTimeMillis()
)

private class ListenSaverRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadVideos(): List<SavedYouTubeVideo> {
        val raw = preferences.getString(VIDEOS_KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        SavedYouTubeVideo(
                            id = item.getString("id"),
                            title = item.optString("title"),
                            url = item.optString("url"),
                            durationMinutes = item.optInt("durationMinutes", 0),
                            targetMegabytes = item.optDouble("targetMegabytes", 10.0),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addVideo(title: String, url: String, durationMinutes: Int, targetMegabytes: Double) {
        val cleanTitle = title.trim()
        val cleanUrl = url.trim()
        if (cleanTitle.isEmpty() || !isYouTubeUrl(cleanUrl) || durationMinutes <= 0 || targetMegabytes <= 0) {
            return
        }

        val updated = loadVideos().toMutableList().apply {
            add(
                SavedYouTubeVideo(
                    id = UUID.randomUUID().toString(),
                    title = cleanTitle,
                    url = cleanUrl,
                    durationMinutes = durationMinutes,
                    targetMegabytes = targetMegabytes
                )
            )
        }
        saveVideos(updated)
    }

    fun deleteVideo(videoId: String) {
        saveVideos(loadVideos().filterNot { it.id == videoId })
    }

    private fun saveVideos(videos: List<SavedYouTubeVideo>) {
        val array = JSONArray()
        videos.forEach { video ->
            array.put(
                JSONObject()
                    .put("id", video.id)
                    .put("title", video.title)
                    .put("url", video.url)
                    .put("durationMinutes", video.durationMinutes)
                    .put("targetMegabytes", video.targetMegabytes)
                    .put("createdAt", video.createdAt)
            )
        }
        preferences.edit().putString(VIDEOS_KEY, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "listen_saver_preferences"
        private const val VIDEOS_KEY = "saved_youtube_videos"
    }
}

@Composable
fun ListenSaverScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { ListenSaverRepository(context.applicationContext) }
    var videos by remember { mutableStateOf(repository.loadVideos()) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun reload() {
        videos = repository.loadVideos()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Listen Saver",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "احفظ فيديوهات YouTube واحسب هل هدف 10 MB واقعي قبل التشغيل",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            DataTargetCard()
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("إضافة فيديو", modifier = Modifier.padding(start = 8.dp))
                }
                FilledTonalButton(
                    onClick = { openDataSaverGuide(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                    Text("إعداد Data saver", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        if (videos.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎧", fontSize = 52.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("لا توجد فيديوهات محفوظة", fontWeight = FontWeight.Bold)
                    Text(
                        text = "أضف رابط YouTube ومدة الفيديو وحدّ البيانات المطلوب",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(videos.sortedByDescending { it.createdAt }, key = { it.id }) { video ->
                SavedVideoCard(
                    video = video,
                    onOpen = { openYouTube(context, video.url) },
                    onDelete = {
                        repository.deleteVideo(video.id)
                        reload()
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddSavedVideoDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, url, duration, target ->
                repository.addVideo(title, url, duration, target)
                reload()
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DataTargetCard() {
    val requiredForHour = requiredBitrateKbps(durationMinutes = 60, targetMegabytes = 10.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Headphones, contentDescription = null)
                Text(
                    text = "هدف 10 MB",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "ساعة واحدة داخل 10 MB تحتاج متوسط بث يقارب ${formatNumber(requiredForHour)} kbps فقط.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "التطبيق لا يستخرج الصوت ولا يحمّل الفيديو. التشغيل يتم في YouTube الرسمي، لذلك لا يمكن ضمان استهلاك 10 MB. فعّل Data saver واختر أقل جودة متاحة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun SavedVideoCard(
    video: SavedYouTubeVideo,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val requiredKbps = requiredBitrateKbps(video.durationMinutes, video.targetMegabytes)
    val difficultTarget = requiredKbps < 32.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text("▶️", fontSize = 26.sp)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${video.durationMinutes} دقيقة • الهدف ${formatNumber(video.targetMegabytes)} MB",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "حذف الفيديو",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (difficultTarget) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "الحد المطلوب: ${formatNumber(requiredKbps)} kbps",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (difficultTarget) {
                            "الهدف شديد الانخفاض، وقد لا يستطيع YouTube الوصول إليه حتى مع Data saver."
                        } else {
                            "الهدف أقرب للواقعية، لكن الاستهلاك الفعلي يعتمد على جودة YouTube والإعلانات والتحميل المسبق."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                Text("فتح في YouTube الرسمي", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun AddSavedVideoDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var targetMb by remember { mutableStateOf("10") }

    val durationValue = duration.toIntOrNull()
    val targetValue = targetMb.replace(',', '.').toDoubleOrNull()
    val validUrl = isYouTubeUrl(url.trim())
    val canSave = title.isNotBlank() && validUrl && durationValue != null && durationValue > 0 && targetValue != null && targetValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة فيديو للاستماع", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم الفيديو") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رابط YouTube") },
                    supportingText = {
                        if (url.isNotBlank() && !validUrl) {
                            Text("أدخل رابط youtube.com أو youtu.be")
                        }
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("مدة الفيديو بالدقائق") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetMb,
                    onValueChange = { value ->
                        targetMb = value.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("حد البيانات المطلوب MB") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                if (durationValue != null && durationValue > 0 && targetValue != null && targetValue > 0) {
                    Text(
                        text = "لتحقيق الهدف يجب ألا يتجاوز متوسط البث ${formatNumber(requiredBitrateKbps(durationValue, targetValue))} kbps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onAdd(title, url.trim(), durationValue ?: 0, targetValue ?: 10.0)
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

private fun requiredBitrateKbps(durationMinutes: Int, targetMegabytes: Double): Double {
    if (durationMinutes <= 0 || targetMegabytes <= 0) return 0.0
    return targetMegabytes * 8_000.0 / (durationMinutes * 60.0)
}

private fun formatNumber(value: Double): String = DecimalFormat("0.#").format(value)

private fun isYouTubeUrl(value: String): Boolean {
    val normalized = value.trim()
    if (normalized.isEmpty()) return false

    return runCatching {
        val uri = Uri.parse(normalized)
        val host = uri.host?.lowercase().orEmpty()
        uri.scheme in setOf("http", "https") && (
            host == "youtu.be" ||
                host == "youtube.com" ||
                host.endsWith(".youtube.com")
            )
    }.getOrDefault(false)
}

private fun openYouTube(context: Context, url: String) {
    val uri = Uri.parse(url)
    val officialAppIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.youtube")
    }

    try {
        context.startActivity(officialAppIntent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun openDataSaverGuide(context: Context) {
    val guideUrl = Uri.parse("https://support.google.com/youtube/answer/91449?hl=en")
    context.startActivity(Intent(Intent.ACTION_VIEW, guideUrl))
}
