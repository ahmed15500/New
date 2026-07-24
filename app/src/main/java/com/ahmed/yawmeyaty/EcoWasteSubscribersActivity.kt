package com.ahmed.yawmeyaty

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahmed.yawmeyaty.ui.theme.YawmeyatyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class EcoWasteSubscribersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val session = readSubscriberSession()
        if (session == null) {
            returnToLogin()
            return
        }

        setContent {
            YawmeyatyTheme {
                SubscribersApp(
                    session = session,
                    onLogout = {
                        getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
                        returnToLogin()
                    }
                )
            }
        }
    }

    private fun readSubscriberSession(): SubscriberSession? {
        val prefs = getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString("token", null) ?: return null
        return SubscriberSession(
            token = token,
            role = prefs.getString("role", "field").orEmpty(),
            name = prefs.getString("name", "مستخدم إيكو ويست").orEmpty()
        )
    }

    private fun returnToLogin() {
        startActivity(
            Intent(this, EcoWasteEntryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    companion object {
        private const val SESSION_PREFS = "eco_waste_phone_session"
    }
}

private data class SubscriberSession(
    val token: String,
    val role: String,
    val name: String
) {
    val isAdmin: Boolean get() = role == "admin"
}

private data class SubscriberRecord(
    val id: String,
    val fullName: String,
    val phone: String?,
    val village: String?,
    val address: String?,
    val notes: String?,
    val lastYear: Int?,
    val lastMonth: Int?,
    val lastPaidOn: String?,
    val activeThisMonth: Boolean
)

private data class SubscriberDraft(
    val id: String? = null,
    val fullName: String,
    val phone: String?,
    val village: String?,
    val address: String?,
    val notes: String?
)

private class SubscribersApi {
    suspend fun loadCustomers(token: String): List<SubscriberRecord> = withContext(Dispatchers.IO) {
        val raw = request(
            method = "GET",
            path = "/rest/v1/eco_waste_customer_status?select=id,full_name,phone,village,address,notes,last_subscription_year,last_subscription_month,last_paid_on,is_subscribed_current_month&order=full_name.asc",
            token = token
        )
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SubscriberRecord(
                        id = item.getString("id"),
                        fullName = item.getString("full_name"),
                        phone = item.optionalString("phone"),
                        village = item.optionalString("village"),
                        address = item.optionalString("address"),
                        notes = item.optionalString("notes"),
                        lastYear = item.optionalInt("last_subscription_year"),
                        lastMonth = item.optionalInt("last_subscription_month"),
                        lastPaidOn = item.optionalString("last_paid_on"),
                        activeThisMonth = item.optBoolean("is_subscribed_current_month", false)
                    )
                )
            }
        }
    }

    suspend fun saveCustomer(token: String, draft: SubscriberDraft): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("full_name", draft.fullName.trim())
            .putOptional("phone", draft.phone)
            .putOptional("village", draft.village)
            .putOptional("address", draft.address)
            .putOptional("notes", draft.notes)
            .toString()

        val raw = if (draft.id == null) {
            request(
                method = "POST",
                path = "/rest/v1/eco_waste_customers",
                token = token,
                body = body,
                prefer = "return=representation"
            )
        } else {
            request(
                method = "PATCH",
                path = "/rest/v1/eco_waste_customers?id=eq.${encode(draft.id)}",
                token = token,
                body = body,
                prefer = "return=representation"
            )
        }
        JSONArray(raw).getJSONObject(0).getString("id")
    }

    suspend fun renewCurrentMonth(token: String, customerId: String) = withContext(Dispatchers.IO) {
        val period = YearMonth.now()
        val body = JSONObject()
            .put("customer_id", customerId)
            .put("subscription_year", period.year)
            .put("subscription_month", period.monthValue)
            .put("paid_on", LocalDate.now().toString())
            .put("notes", "تجديد من تطبيق إيكو ويست")
            .toString()

        request(
            method = "POST",
            path = "/rest/v1/eco_waste_subscriptions?on_conflict=customer_id,subscription_year,subscription_month",
            token = token,
            body = body,
            prefer = "resolution=merge-duplicates,return=minimal"
        )
    }

    private fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null,
        prefer: String? = null
    ): String {
        val connection = URL(SUPABASE_URL + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 20_000
        connection.readTimeout = 25_000
        connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        prefer?.let { connection.setRequestProperty("Prefer", it) }

        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (status !in 200..299) {
            val source = runCatching {
                val json = JSONObject(response)
                json.optString("message").ifBlank {
                    json.optString("msg").ifBlank { json.optString("error_description") }
                }
            }.getOrNull().orEmpty()
            throw IllegalStateException(source.ifBlank { "تعذر الاتصال بالخدمة. رمز الخطأ: $status" })
        }
        return response
    }

    companion object {
        private const val SUPABASE_URL = "https://pqevttogkdjyedljtyyd.supabase.co"
        private const val PUBLISHABLE_KEY = "sb_publishable_asDp-LKRawMRSaPIQi9i6w_dm0XTBvw"

        private fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscribersApp(session: SubscriberSession, onLogout: () -> Unit) {
    val api = remember { SubscribersApi() }
    var customers by remember { mutableStateOf(emptyList<SubscriberRecord>()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<SubscriberRecord?>(null) }
    var editing by remember { mutableStateOf<SubscriberRecord?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            runCatching { api.loadCustomers(session.token) }
                .onSuccess { customers = it }
                .onFailure { message = it.message ?: "تعذر تحميل المشتركين." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }

    val filtered = remember(customers, query) {
        filterSubscribers(customers, query)
    }
    val activeCount = customers.count { it.activeThisMonth }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("سجل المشتركين", fontWeight = FontWeight.Black)
                        Text(
                            "${session.name} • ${if (session.isAdmin) "أدمن" else "مسؤول ميداني"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = ::refresh, enabled = !loading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "تحديث")
                    }
                    TextButton(onClick = onLogout) {
                        Icon(Icons.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("تسجيل خروج", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            if (session.isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("إضافة مشترك") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("اشتراكات ${monthLabel()}", fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي الأسماء: ${customers.size}")
                            Text("مشترك حاليًا: $activeCount")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ابحث بالاسم أو رقم التليفون") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    supportingText = {
                        if (query.isNotBlank()) Text("عدد النتائج: ${filtered.size}")
                    },
                    singleLine = true
                )
            }
            if (loading && customers.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text(
                        "لا توجد نتائج مطابقة للاسم أو رقم التليفون.",
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filtered, key = { it.id }) { customer ->
                    SubscriberCard(customer = customer, onClick = { selected = customer })
                }
            }
        }
    }

    selected?.let { customer ->
        SubscriberDetailsDialog(
            customer = customer,
            isAdmin = session.isAdmin,
            onDismiss = { selected = null },
            onEdit = {
                selected = null
                editing = customer
            },
            onRenew = {
                scope.launch {
                    loading = true
                    runCatching { api.renewCurrentMonth(session.token, customer.id) }
                        .onSuccess {
                            selected = null
                            message = "تم تجديد اشتراك ${customer.fullName} للشهر الحالي."
                            refresh()
                        }
                        .onFailure { message = it.message ?: "تعذر تجديد الاشتراك." }
                    loading = false
                }
            }
        )
    }

    if (showAdd) {
        SubscriberEditorDialog(
            title = "إضافة مشترك جديد",
            initial = null,
            allowSubscribe = true,
            onDismiss = { showAdd = false },
            onSave = { draft, subscribe ->
                scope.launch {
                    loading = true
                    runCatching {
                        val id = api.saveCustomer(session.token, draft)
                        if (subscribe) api.renewCurrentMonth(session.token, id)
                    }.onSuccess {
                        showAdd = false
                        message = "تم حفظ المشترك بنجاح."
                        refresh()
                    }.onFailure { message = it.message ?: "تعذر حفظ المشترك." }
                    loading = false
                }
            }
        )
    }

    editing?.let { customer ->
        SubscriberEditorDialog(
            title = "تعديل بيانات المشترك",
            initial = customer,
            allowSubscribe = false,
            onDismiss = { editing = null },
            onSave = { draft, _ ->
                scope.launch {
                    loading = true
                    runCatching { api.saveCustomer(session.token, draft) }
                        .onSuccess {
                            editing = null
                            message = "تم تحديث بيانات المشترك."
                            refresh()
                        }
                        .onFailure { message = it.message ?: "تعذر تحديث البيانات." }
                    loading = false
                }
            }
        )
    }
}

@Composable
private fun SubscriberCard(customer: SubscriberRecord, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (customer.activeThisMonth) Color(0xFFE7F7EC)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (customer.activeThisMonth) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                contentDescription = null,
                tint = if (customer.activeThisMonth) Color(0xFF087F3C) else MaterialTheme.colorScheme.error
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(customer.fullName, fontWeight = FontWeight.Black)
                Text(
                    customer.phone ?: "لا يوجد رقم تليفون",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                customer.village?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
            }
            Text(
                if (customer.activeThisMonth) "مشترك" else "غير مشترك",
                fontWeight = FontWeight.Bold,
                color = if (customer.activeThisMonth) Color(0xFF087F3C) else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SubscriberDetailsDialog(
    customer: SubscriberRecord,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRenew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(customer.fullName, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (customer.activeThisMonth) Color(0xFFE7F7EC)
                    else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        if (customer.activeThisMonth) "مشترك في الشهر الحالي" else "غير مشترك في الشهر الحالي",
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }
                Text("رقم التليفون: ${customer.phone ?: "غير مسجل"}")
                Text("القرية/المنطقة: ${customer.village ?: "غير مسجلة"}")
                customer.address?.let { Text("العنوان: $it") }
                Text("آخر اشتراك: ${lastSubscriptionLabel(customer)}")
                customer.notes?.let { Text("ملاحظات: $it") }
            }
        },
        confirmButton = {
            if (isAdmin && !customer.activeThisMonth) {
                Button(onClick = onRenew) { Text("تجديد الشهر الحالي") }
            } else {
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        },
        dismissButton = {
            if (isAdmin) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("تعديل")
                }
            }
        }
    )
}

@Composable
private fun SubscriberEditorDialog(
    title: String,
    initial: SubscriberRecord?,
    allowSubscribe: Boolean,
    onDismiss: () -> Unit,
    onSave: (SubscriberDraft, Boolean) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.fullName.orEmpty()) }
    var phone by remember(initial?.id) { mutableStateOf(initial?.phone.orEmpty()) }
    var village by remember(initial?.id) { mutableStateOf(initial?.village.orEmpty()) }
    var address by remember(initial?.id) { mutableStateOf(initial?.address.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var subscribe by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("الاسم الكامل *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = normalizeDigits(it).take(11) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رقم التليفون") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("القرية أو المنطقة") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("العنوان") },
                    minLines = 2
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظات") },
                    minLines = 2
                )
                if (allowSubscribe) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = subscribe, onCheckedChange = { subscribe = it })
                        Text("تسجيل اشتراك الشهر الحالي")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        SubscriberDraft(
                            id = initial?.id,
                            fullName = name,
                            phone = phone.takeIf { it.isNotBlank() },
                            village = village.takeIf { it.isNotBlank() },
                            address = address.takeIf { it.isNotBlank() },
                            notes = notes.takeIf { it.isNotBlank() }
                        ),
                        subscribe
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

private fun filterSubscribers(
    customers: List<SubscriberRecord>,
    rawQuery: String
): List<SubscriberRecord> {
    if (rawQuery.isBlank()) return customers

    val nameQuery = normalizeArabic(rawQuery)
    val phoneQuery = normalizeDigits(rawQuery)

    return customers.filter { customer ->
        val matchesName = nameQuery.isNotBlank() &&
            normalizeArabic(customer.fullName).contains(nameQuery)
        val matchesPhone = phoneQuery.isNotBlank() &&
            normalizeDigits(customer.phone.orEmpty()).contains(phoneQuery)
        matchesName || matchesPhone
    }
}

private fun normalizeArabic(value: String): String = value
    .trim()
    .lowercase(Locale("ar"))
    .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    .replace('أ', 'ا')
    .replace('إ', 'ا')
    .replace('آ', 'ا')
    .replace('ى', 'ي')
    .replace('ة', 'ه')
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun normalizeDigits(value: String): String = buildString {
    value.forEach { character ->
        when (character) {
            in '0'..'9' -> append(character)
            in '٠'..'٩' -> append(('0'.code + (character.code - '٠'.code)).toChar())
            in '۰'..'۹' -> append(('0'.code + (character.code - '۰'.code)).toChar())
        }
    }
}

private fun JSONObject.optionalString(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

private fun JSONObject.optionalInt(key: String): Int? =
    if (isNull(key)) null else optInt(key)

private fun JSONObject.putOptional(key: String, value: String?): JSONObject =
    if (value.isNullOrBlank()) put(key, JSONObject.NULL) else put(key, value.trim())

private fun monthLabel(): String =
    YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))

private fun lastSubscriptionLabel(customer: SubscriberRecord): String {
    val year = customer.lastYear ?: return "لا يوجد اشتراك سابق"
    val month = customer.lastMonth ?: return "لا يوجد اشتراك سابق"
    val period = YearMonth.of(year, month)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))
    return customer.lastPaidOn?.let { "$period — $it" } ?: period
}