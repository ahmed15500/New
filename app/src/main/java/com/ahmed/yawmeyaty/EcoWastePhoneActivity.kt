package com.ahmed.yawmeyaty

import android.content.Context
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class EcoWastePhoneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YawmeyatyTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    EcoWastePhoneApp()
                }
            }
        }
    }
}

private data class PhoneSession(
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val role: String,
    val displayName: String
) {
    val isAdmin: Boolean get() = role == "admin"
}

private data class PhoneCustomer(
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

private data class PhoneCustomerDraft(
    val id: String? = null,
    val fullName: String,
    val phone: String?,
    val village: String?,
    val address: String?,
    val notes: String?
)

private class PhoneSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("eco_waste_phone_session", Context.MODE_PRIVATE)

    fun load(): PhoneSession? {
        val token = prefs.getString("token", null) ?: return null
        return PhoneSession(
            email = prefs.getString("email", "").orEmpty(),
            accessToken = token,
            refreshToken = prefs.getString("refresh", "").orEmpty(),
            role = prefs.getString("role", "field").orEmpty(),
            displayName = prefs.getString("name", "مستخدم إيكو ويست").orEmpty()
        )
    }

    fun save(session: PhoneSession) {
        prefs.edit()
            .putString("email", session.email)
            .putString("token", session.accessToken)
            .putString("refresh", session.refreshToken)
            .putString("role", session.role)
            .putString("name", session.displayName)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()
}

private class PhoneEcoWasteApi {
    suspend fun login(phoneInput: String, password: String): PhoneSession = withContext(Dispatchers.IO) {
        val phone = normalizeEgyptianPhone(phoneInput)
        require(phone.matches(Regex("^01[0125]\\d{8}$"))) {
            "اكتب رقم تليفون مصري صحيح مكوّن من 11 رقمًا."
        }

        // The user only sees and enters the phone number. The internal alias is never displayed.
        val loginEmail = "$phone@ecowaste.local"
        val body = JSONObject()
            .put("email", loginEmail)
            .put("password", password)
            .toString()

        val authRaw = request(
            method = "POST",
            path = "/auth/v1/token?grant_type=password",
            body = body
        )
        val auth = JSONObject(authRaw)
        val accessToken = auth.getString("access_token")
        val refreshToken = auth.optString("refresh_token")
        val userEmail = auth.getJSONObject("user").optString("email", loginEmail)

        val accessRaw = request(
            method = "GET",
            path = "/rest/v1/eco_waste_access?select=role,display_name&email=eq.${encode(userEmail)}",
            token = accessToken
        )
        val rows = JSONArray(accessRaw)
        require(rows.length() > 0) { "هذا الرقم غير مصرح له باستخدام التطبيق." }
        val access = rows.getJSONObject(0)

        PhoneSession(
            email = userEmail,
            accessToken = accessToken,
            refreshToken = refreshToken,
            role = access.getString("role"),
            displayName = access.optString("display_name").ifBlank { phone }
        )
    }

    suspend fun loadCustomers(token: String): List<PhoneCustomer> = withContext(Dispatchers.IO) {
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
                    PhoneCustomer(
                        id = item.getString("id"),
                        fullName = item.getString("full_name"),
                        phone = item.nullableString("phone"),
                        village = item.nullableString("village"),
                        address = item.nullableString("address"),
                        notes = item.nullableString("notes"),
                        lastYear = item.nullableInt("last_subscription_year"),
                        lastMonth = item.nullableInt("last_subscription_month"),
                        lastPaidOn = item.nullableString("last_paid_on"),
                        activeThisMonth = item.optBoolean("is_subscribed_current_month", false)
                    )
                )
            }
        }
    }

    suspend fun saveCustomer(token: String, draft: PhoneCustomerDraft): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("full_name", draft.fullName.trim())
            .putNullable("phone", draft.phone)
            .putNullable("village", draft.village)
            .putNullable("address", draft.address)
            .putNullable("notes", draft.notes)
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
        token: String? = null,
        body: String? = null,
        prefer: String? = null
    ): String {
        val connection = URL(SUPABASE_URL + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 20_000
        connection.readTimeout = 25_000
        connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
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
            val sourceMessage = runCatching {
                val json = JSONObject(response)
                json.optString("msg").ifBlank {
                    json.optString("message").ifBlank { json.optString("error_description") }
                }
            }.getOrNull().orEmpty()
            val message = when {
                sourceMessage.contains("Invalid login credentials", ignoreCase = true) -> "رقم التليفون أو كلمة المرور غير صحيحة."
                sourceMessage.contains("Email not confirmed", ignoreCase = true) -> "الحساب لم يتم تفعيله بعد."
                sourceMessage.isNotBlank() -> sourceMessage
                else -> "تعذر الاتصال بالخدمة. رمز الخطأ: $status"
            }
            throw IllegalStateException(message)
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

private fun normalizeEgyptianPhone(value: String): String {
    val digits = value.filter(Char::isDigit)
    return when {
        digits.startsWith("0020") && digits.length == 14 -> "0${digits.drop(4)}"
        digits.startsWith("20") && digits.length == 12 -> "0${digits.drop(2)}"
        digits.length == 10 && digits.startsWith("1") -> "0$digits"
        else -> digits
    }
}

private fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

private fun JSONObject.nullableInt(key: String): Int? =
    if (isNull(key)) null else optInt(key)

private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
    if (value.isNullOrBlank()) put(key, JSONObject.NULL) else put(key, value.trim())

@Composable
private fun EcoWastePhoneApp() {
    val context = LocalContext.current
    val api = remember { PhoneEcoWasteApi() }
    val store = remember { PhoneSessionStore(context.applicationContext) }
    var session by remember { mutableStateOf(store.load()) }
    var customers by remember { mutableStateOf(emptyList<PhoneCustomer>()) }
    var loading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<PhoneCustomer?>(null) }
    var editing by remember { mutableStateOf<PhoneCustomer?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    fun refresh() {
        val current = session ?: return
        scope.launch {
            loading = true
            runCatching { api.loadCustomers(current.accessToken) }
                .onSuccess { customers = it }
                .onFailure {
                    message = it.message ?: "تعذر تحميل المشتركين."
                    if (it.message?.contains("JWT", true) == true) {
                        store.clear()
                        session = null
                    }
                }
            loading = false
        }
    }

    LaunchedEffect(session?.accessToken) {
        if (session != null) refresh()
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }

    if (session == null) {
        PhoneLoginScreen(
            loading = loading,
            error = loginError,
            onLogin = { phone, password ->
                scope.launch {
                    loading = true
                    loginError = null
                    runCatching { api.login(phone, password) }
                        .onSuccess {
                            store.save(it)
                            session = it
                        }
                        .onFailure { loginError = it.message ?: "تعذر تسجيل الدخول." }
                    loading = false
                }
            }
        )
        return
    }

    val current = session ?: return
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            PhoneTopBar(
                session = current,
                loading = loading,
                onRefresh = ::refresh,
                onLogout = {
                    store.clear()
                    customers = emptyList()
                    session = null
                }
            )
        },
        floatingActionButton = {
            if (current.isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("إضافة مشترك") }
                )
            }
        }
    ) { padding ->
        PhoneCustomerList(
            modifier = Modifier.padding(padding),
            customers = customers,
            loading = loading,
            isAdmin = current.isAdmin,
            onSelect = { selected = it }
        )
    }

    selected?.let { customer ->
        PhoneCustomerDialog(
            customer = customer,
            isAdmin = current.isAdmin,
            onDismiss = { selected = null },
            onEdit = {
                selected = null
                editing = customer
            },
            onRenew = {
                scope.launch {
                    loading = true
                    runCatching { api.renewCurrentMonth(current.accessToken, customer.id) }
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
        PhoneCustomerEditor(
            title = "إضافة مشترك جديد",
            initial = null,
            showSubscribe = true,
            onDismiss = { showAdd = false },
            onSave = { draft, subscribe ->
                scope.launch {
                    loading = true
                    runCatching {
                        val id = api.saveCustomer(current.accessToken, draft)
                        if (subscribe) api.renewCurrentMonth(current.accessToken, id)
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
        PhoneCustomerEditor(
            title = "تعديل بيانات المشترك",
            initial = customer,
            showSubscribe = false,
            onDismiss = { editing = null },
            onSave = { draft, _ ->
                scope.launch {
                    loading = true
                    runCatching { api.saveCustomer(current.accessToken, draft) }
                        .onSuccess {
                            editing = null
                            message = "تم تحديث البيانات."
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
private fun PhoneLoginScreen(
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("♻️", fontSize = 72.sp)
        Text(
            text = "إيكو ويست",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "متابعة اشتراكات جمع المخلفات",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { value ->
                phone = value.filter { it.isDigit() || it == '+' }.take(15)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("رقم التليفون") },
            placeholder = { Text("مثال: 01206392890") },
            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("كلمة المرور") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        error?.let {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            enabled = !loading && phone.filter(Char::isDigit).length >= 10 && password.isNotBlank(),
            onClick = { onLogin(phone, password) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("تسجيل الدخول", fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "الدخول متاح فقط للحسابات التي يضيفها مدير النظام.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneTopBar(
    session: PhoneSession,
    loading: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text("إيكو ويست", fontWeight = FontWeight.Black)
                Text(
                    text = "${session.displayName} • ${if (session.isAdmin) "مدير" else "مسؤول ميداني"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Rounded.Refresh, contentDescription = "تحديث")
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Rounded.Logout, contentDescription = "تسجيل الخروج")
            }
        }
    )
}

@Composable
private fun PhoneCustomerList(
    modifier: Modifier,
    customers: List<PhoneCustomer>,
    loading: Boolean,
    isAdmin: Boolean,
    onSelect: (PhoneCustomer) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalized = normalizeSearch(query)
    val filtered = remember(customers, normalized) {
        if (normalized.isBlank()) customers
        else customers.filter {
            normalizeSearch(it.fullName).contains(normalized) ||
                it.phone.orEmpty().filter(Char::isDigit).contains(query.filter(Char::isDigit))
        }
    }
    val active = customers.count { it.activeThisMonth }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                    Text("اشتراكات ${currentMonthLabel()}", fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي الأسماء: ${customers.size}")
                        Text("مشترك حاليًا: $active")
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
                singleLine = true
            )
        }
        if (loading && customers.isEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Text(
                    text = "لا توجد نتائج مطابقة.",
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(filtered, key = { it.id }) { customer ->
                PhoneCustomerCard(customer = customer, onClick = { onSelect(customer) })
            }
        }
        if (isAdmin) {
            item { Spacer(Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun PhoneCustomerCard(customer: PhoneCustomer, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (customer.activeThisMonth) {
                Color(0xFFE7F7EC)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            }
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
                customer.village?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
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
private fun PhoneCustomerDialog(
    customer: PhoneCustomer,
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
                StatusSurface(customer.activeThisMonth)
                Text("رقم التليفون: ${customer.phone ?: "غير مسجل"}")
                Text("القرية/المنطقة: ${customer.village ?: "غير مسجلة"}")
                customer.address?.let { Text("العنوان: $it") }
                Text("آخر اشتراك: ${lastPeriodLabel(customer)}")
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
                    Text("تعديل")
                }
            }
        }
    )
}

@Composable
private fun StatusSurface(active: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (active) Color(0xFFE7F7EC) else MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = if (active) "مشترك في الشهر الحالي" else "غير مشترك في الشهر الحالي",
            modifier = Modifier.padding(12.dp),
            fontWeight = FontWeight.Black,
            color = if (active) Color(0xFF087F3C) else MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhoneCustomerEditor(
    title: String,
    initial: PhoneCustomer?,
    showSubscribe: Boolean,
    onDismiss: () -> Unit,
    onSave: (PhoneCustomerDraft, Boolean) -> Unit
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
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاسم الكامل *") }, singleLine = true)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(11) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رقم التليفون") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(village, { village = it }, modifier = Modifier.fillMaxWidth(), label = { Text("القرية أو المنطقة") }, singleLine = true)
                OutlinedTextField(address, { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text("العنوان") }, minLines = 2)
                OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("ملاحظات") }, minLines = 2)
                if (showSubscribe) {
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
                        PhoneCustomerDraft(
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

private fun normalizeSearch(value: String): String = value
    .trim()
    .lowercase(Locale("ar"))
    .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    .replace('أ', 'ا')
    .replace('إ', 'ا')
    .replace('آ', 'ا')
    .replace('ى', 'ي')
    .replace('ة', 'ه')
    .replace(Regex("\\s+"), " ")

private fun currentMonthLabel(): String =
    YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))

private fun lastPeriodLabel(customer: PhoneCustomer): String {
    val year = customer.lastYear ?: return "لا يوجد اشتراك سابق"
    val month = customer.lastMonth ?: return "لا يوجد اشتراك سابق"
    val period = YearMonth.of(year, month)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))
    return customer.lastPaidOn?.let { "$period — $it" } ?: period
}
