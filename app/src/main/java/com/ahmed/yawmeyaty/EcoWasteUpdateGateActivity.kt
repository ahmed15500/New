package com.ahmed.yawmeyaty

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahmed.yawmeyaty.ui.theme.YawmeyatyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class EcoWasteUpdateGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YawmeyatyTheme {
                UpdateGateScreen(
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    openApp = {
                        startActivity(Intent(this, EcoWasteEntryActivity::class.java))
                        finish()
                    },
                    openDownload = { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    closeApp = { finishAffinity() }
                )
            }
        }
    }
}

private data class RemoteUpdateConfig(
    val minimumVersionCode: Int,
    val latestVersionName: String,
    val forceUpdate: Boolean,
    val title: String,
    val message: String,
    val downloadUrl: String?
)

private suspend fun loadRemoteUpdateConfig(): RemoteUpdateConfig? = withContext(Dispatchers.IO) {
    val connection = URL(
        "$SUPABASE_URL/rest/v1/eco_waste_app_config" +
            "?select=minimum_version_code,latest_version_name,force_update,update_title,update_message,download_url&id=eq.1"
    ).openConnection() as HttpURLConnection

    connection.requestMethod = "GET"
    connection.connectTimeout = 10_000
    connection.readTimeout = 12_000
    connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
    connection.setRequestProperty("Accept", "application/json")

    val status = connection.responseCode
    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
    val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
    connection.disconnect()

    if (status !in 200..299 || response.isBlank()) return@withContext null
    val rows = JSONArray(response)
    if (rows.length() == 0) return@withContext null
    val row = rows.getJSONObject(0)

    RemoteUpdateConfig(
        minimumVersionCode = row.optInt("minimum_version_code", 1),
        latestVersionName = row.optString("latest_version_name", ""),
        forceUpdate = row.optBoolean("force_update", false),
        title = row.optString("update_title", "تحديث التطبيق مطلوب"),
        message = row.optString("update_message", "يرجى تحديث التطبيق إلى أحدث إصدار."),
        downloadUrl = if (row.isNull("download_url")) null else row.optString("download_url").trim().ifBlank { null }
    )
}

@Composable
private fun UpdateGateScreen(
    currentVersionCode: Int,
    openApp: () -> Unit,
    openDownload: (String) -> Unit,
    closeApp: () -> Unit
) {
    var config by remember { mutableStateOf<RemoteUpdateConfig?>(null) }
    var checking by remember { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        checking = true
        config = runCatching { loadRemoteUpdateConfig() }.getOrNull()
        checking = false

        val remote = config
        val updateRequired = remote != null &&
            remote.forceUpdate &&
            currentVersionCode < remote.minimumVersionCode

        if (!updateRequired) openApp()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.SystemUpdate,
            contentDescription = null,
            modifier = Modifier.size(70.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(18.dp))
        Text("إيكو ويست", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        if (checking) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("جارٍ التحقق من إصدار التطبيق…", textAlign = TextAlign.Center)
        }
    }

    val remote = config
    val updateRequired = remote != null &&
        remote.forceUpdate &&
        currentVersionCode < remote.minimumVersionCode

    if (!checking && updateRequired && remote != null) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
            title = { Text(remote.title, fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(remote.message)
                    if (remote.latestVersionName.isNotBlank()) {
                        Text(
                            "الإصدار المطلوب: ${remote.latestVersionName}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                if (remote.downloadUrl != null) {
                    Button(
                        onClick = { openDownload(remote.downloadUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("تحميل التحديث") }
                } else {
                    Button(
                        onClick = closeApp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("إغلاق التطبيق") }
                }
            },
            dismissButton = {
                TextButton(onClick = { attempt += 1 }) { Text("إعادة الفحص") }
            }
        )
    }
}

private const val SUPABASE_URL = "https://pqevttogkdjyedljtyyd.supabase.co"
private const val PUBLISHABLE_KEY = "sb_publishable_asDp-LKRawMRSaPIQi9i6w_dm0XTBvw"
