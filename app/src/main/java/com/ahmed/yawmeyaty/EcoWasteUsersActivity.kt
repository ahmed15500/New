package com.ahmed.yawmeyaty

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class EcoWasteUsersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString("token", null).orEmpty()
        val role = prefs.getString("role", "field").orEmpty()

        setContent {
            YawmeyatyTheme {
                UserAccountsScreen(
                    token = token,
                    isAdmin = role == "admin",
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        private const val SESSION_PREFS = "eco_waste_phone_session"
    }
}

private data class EcoWasteUserAccount(
    val id: String,
    val phone: String,
    val displayName: String,
    val role: String,
    val createdAt: String,
    val lastSignInAt: String?,
    val disabled: Boolean
)

private suspend fun loadEcoWasteUserAccounts(token: String): List<EcoWasteUserAccount> =
    withContext(Dispatchers.IO) {
        if (token.isBlank()) throw IllegalStateException("سجّل الدخول مرة أخرى.")

        val connection = URL("https://pqevttogkdjyedljtyyd.supabase.co/functions/v1/eco-waste-list-users")
            .openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 20_000
        connection.readTimeout = 25_000
        connection.setRequestProperty("Authorization", "Bearer $token")

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (status !in 200..299) {
            val source = runCatching { JSONObject(response).optString("error") }.getOrNull().orEmpty()
            val message = when (source) {
                "admin_required" -> "عرض الحسابات متاح للمدير فقط."
                "unauthorized" -> "انتهت جلسة الدخول. سجّل الدخول مرة أخرى."
                else -> source.ifBlank { "تعذر تحميل الحسابات. رمز الخطأ: $status" }
            }
            throw IllegalStateException(message)
        }

        val array = JSONObject(response).optJSONArray("accounts")
            ?: throw IllegalStateException("استجابة الحسابات غير صالحة.")

        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    EcoWasteUserAccount(
                        id = item.optString("id"),
                        phone = item.optString("phone"),
                        displayName = item.optString("display_name"),
                        role = item.optString("role", "unassigned"),
                        createdAt = item.optString("created_at"),
                        lastSignInAt = item.optString("last_sign_in_at").takeIf {
                            it.isNotBlank() && it != "null"
                        },
                        disabled = item.optBoolean("disabled", false)
                    )
                )
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserAccountsScreen(
    token: String,
    isAdmin: Boolean,
    onBack: () -> Unit
) {
    var accounts by remember { mutableStateOf<List<EcoWasteUserAccount>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("all") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey, token, isAdmin) {
        if (!isAdmin) {
            error = "عرض الحسابات متاح للمدير فقط."
            return@LaunchedEffect
        }

        loading = true
        error = ""
        runCatching { loadEcoWasteUserAccounts(token) }
            .onSuccess { accounts = it }
            .onFailure { error = it.message ?: "تعذر تحميل الحسابات." }
        loading = false
    }

    val filtered = remember(accounts, query, roleFilter) {
        val normalizedQuery = query.trim()
        accounts.filter { account ->
            val matchesRole = roleFilter == "all" || account.role == roleFilter
            val matchesQuery =
                normalizedQuery.isBlank() ||
                    account.displayName.contains(normalizedQuery, ignoreCase = true) ||
                    account.phone.contains(normalizedQuery)
            matchesRole && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("مستخدمو البرنامج", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(enabled = !loading && isAdmin, onClick = { reloadKey += 1 }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "تحديث")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { AccountStats(accounts) }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ابحث بالاسم أو رقم الهاتف") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleFilterChip("الكل", "all", roleFilter) { roleFilter = it }
                    RoleFilterChip("المديرون", "admin", roleFilter) { roleFilter = it }
                    RoleFilterChip("المسؤولون الميدانيون", "field", roleFilter) { roleFilter = it }
                    RoleFilterChip("بدون صلاحية", "unassigned", roleFilter) { roleFilter = it }
                }
            }

            if (loading && accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }

            if (error.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(error, textAlign = TextAlign.Center)
                            if (isAdmin) {
                                OutlinedButton(onClick = { reloadKey += 1 }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("إعادة المحاولة")
                                }
                            }
                        }
                    }
                }
            }

            if (!loading && error.isBlank() && filtered.isEmpty()) {
                item {
                    Text(
                        "لا توجد حسابات مطابقة.",
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(filtered, key = { it.id }) { account ->
                UserAccountCard(account)
            }
        }
    }
}

@Composable
private fun AccountStats(accounts: List<EcoWasteUserAccount>) {
    val admins = accounts.count { it.role == "admin" }
    val fieldUsers = accounts.count { it.role == "field" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.People, contentDescription = null, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("إجمالي الحسابات", fontWeight = FontWeight.Black)
                    Text("${accounts.size} حسابًا مسجلًا")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AccountStatValue("مدير", admins)
                AccountStatValue("ميداني", fieldUsers)
                AccountStatValue("أخرى", accounts.size - admins - fieldUsers)
            }
        }
    }
}

@Composable
private fun AccountStatValue(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun RoleFilterChip(label: String, value: String, selected: String, onSelect: (String) -> Unit) {
    FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) })
}

@Composable
private fun UserAccountCard(account: EcoWasteUserAccount) {
    val roleLabel = when (account.role) {
        "admin" -> "مدير"
        "field" -> "مسؤول ميداني"
        else -> "بدون صلاحية"
    }
    val roleIcon = if (account.role == "admin") Icons.Rounded.AdminPanelSettings else Icons.Rounded.Person

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(roleIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    account.displayName.ifBlank { "مستخدم بدون اسم" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(account.phone.ifBlank { "رقم الهاتف غير متاح" })
                Text(roleLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(
                    "تاريخ الإنشاء: ${formatAccountDate(account.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "آخر دخول: ${account.lastSignInAt?.let(::formatAccountDate) ?: "لم يسجل دخولًا بعد"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (account.disabled) {
                    Text("الحساب معطّل", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatAccountDate(value: String): String = runCatching {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy - hh:mm a", Locale("ar", "EG"))
    Instant.parse(value).atZone(ZoneId.systemDefault()).format(formatter)
}.getOrDefault(value.take(16).replace("T", " "))
