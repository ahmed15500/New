package com.ahmed.yawmeyaty

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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

class EcoWasteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YawmeyatyTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    EcoWasteApp()
                }
            }
        }
    }
}

private data class UserSession(
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val role: String,
    val displayName: String
) {
    val isAdmin: Boolean get() = role == "admin"
}

private data class CustomerStatus(
    val id: String,
    val fullName: String,
    val phone: String?,
    val village: String?,
    val address: String?,
    val notes: String?,
    val lastSubscriptionYear: Int?,    val lastSubscriptionMonth: Int?,
    val lastPaidOn: String?,
    val isSubscribedCurrentMonth: Boolean
)

private data class CustomerDraft(
    val id: String? = null,
    val fullName: String,
    val phone: String?,
    val village: String?,
    val address: String?,
    val notes: String?
)

private class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("eco_waste_session", Context.MODE_PRIVATE)

    fun load(): UserSession? {
        val email = preferences.getString("email", null) ?: return null
        val accessToken = preferences.getString("access_token", null) ?: return null
        return UserSession(
            email = email,
            accessToken = accessToken,
            refreshToken = preferences.getString("refresh_token", "").orEmpty(),
            role = preferences.getString("role", "field").orEmpty(),
            displayName = preferences.getString("display_name", email).orEmpty()
        )
    }

    fun save(session: UserSession) {
        preferences.edit()
            .putString("email", session.email)
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("role", session.role)
            .putString("display_name", session.displayName)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}

private class EcoWasteApi {
    suspend fun login(email: String, password: String): UserSession = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("email", email.trim())
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
        val userEmail = auth.getJSONObject("user").optString("email", email.trim())

        val accessRaw = request(
            method = "GET",
            path = "/rest/v1/eco_waste_access?select=role,display_name&email=eq.${encode(userEmail)}",
            token = accessToken
        )
        val accessRows = JSONArray(accessRaw)
        require(accessRows.length() > 0) { "هذا الحساب غير مصرح له باستخدام تطبيق إيكو ويست." }
        val access = accessRows.getJSONObject(0)
        UserSession(
            email = userEmail,
            accessToken = accessToken,
            refreshToken = refreshToken,
            role = access.getString("role"),
            displayName = access.optString("display_name").ifBlank { userEmail }
        )
    }

    suspend fun loadCustomers(token: String): List<CustomerStatus> = withContext(Dispatchers.IO) {
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
                    CustomerStatus(
                        id = item.getString("id"),
                        fullName = item.getString("full_name"),
                        phone = item.nullableString("phone"),
                        village = item.nullableString("village"),
                        address = item.nullableString("address"),
                        notes = item.nullableString("notes"),
                        lastSubscriptionYear = item.nullableInt("last_subscription_year"),
                        lastSubscriptionMonth = item.nullableInt("last_subscription_month"),
                        lastPaidOn = item.nullableString("last_paid_on"),
                        isSubscribedCurrentMonth = item.optBoolean("is_subscribed_current_month", false)
                    )
                )
            }
        }
    }

    suspend fun saveCustomer(token: String, draft: CustomerDraft): CustomerStatus = withContext(Dispatchers.IO) {
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
        val customer = JSONArray(raw).getJSONObject(0)
        CustomerStatus(
            id = customer.getString("id"),
            fullName = customer.getString("full_name"),
            phone = customer.nullableString("phone"),
            village = customer.nullableString("village"),
            address = customer.nullableString("address"),
            notes = customer.nullableString("notes"),
            lastSubscriptionYear = null,            lastSubscriptionMonth = null,
            lastPaidOn = null,
            isSubscribedCurrentMonth = false
        )
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
            val message = runCatching {
                val json = JSONObject(response)
                json.optString("msg").ifBlank {
                    json.optString("message").ifBlank { json.optString("error_description") }
                }
            }.getOrNull().orEmpty().ifBlank { "تعذر الاتصال بالخدمة. رمز الخطأ: $status" }
            throw IllegalStateException(message)
        }
        return response
    }

    companion object {
        private const val SUPABASE_URL = "https://nxbakauhojxgavjnpsia.supabase.co"
        private const val PUBLISHABLE_KEY = "sb_publishable_aYaJtX2b-RrDODRJnQr_6Q_nkNmKE9R"

        private fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}

private fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

private fun JSONObject.nullableInt(key: String): Int? =
    if (isNull(key)) null else optInt(key)

private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
    if (value.isNullOrBlank()) put(key, JSONObject.NULL) else put(key, value.trim())

@Composable
private fun EcoWasteApp() {
    val context = LocalContext.current
    val api = remember { EcoWasteApi() }
    val store = remember { SessionStore(context.applicationContext) }
    var session by remember { mutableStateOf(store.load()) }
    var customers by remember { mutableStateOf(emptyList<CustomerStatus>()) }
    var loading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var selectedCustomer by remember { mutableStateOf<CustomerStatus?>(null) }
    var editingCustomer by remember { mutableStateOf<CustomerStatus?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshCustomers() {
        val current = session ?: return
        scope.launch {
            loading = true
            runCatching { api.loadCustomers(current.accessToken) }
                .onSuccess { customers = it }
                .onFailure { error ->
                    snackbarMessage = error.message ?: "تعذر تحميل المشتركين."
                    if (error.message?.contains("JWT", ignoreCase = true) == true) {
                        store.clear()
                        session = null
                    }
                }
            loading = false
        }
    }

    LaunchedEffect(session?.accessToken) {
        if (session != null) refreshCustomers()
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    if (session == null) {
        LoginScreen(
            loading = loading,
            error = loginError,
            onLogin = { email, password ->
                scope.launch {
                    loading = true
                    loginError = null
                    runCatching { api.login(email, password) }
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

    val currentSession = session ?: return
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EcoWasteTopBar(
                session = currentSession,
                loading = loading,
                onRefresh = ::refreshCustomers,
                onLogout = {
                    store.clear()
                    customers = emptyList()
                    session = null
                }
            )
        },
        floatingActionButton = {
            if (currentSession.isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("إضافة مشترك") }
                )
            }
        }
    ) { innerPadding ->
        CustomerSearchScreen(
            modifier = Modifier.padding(innerPadding),
            session = currentSession,
            customers = customers,
            loading = loading,
            onCustomerClick = { selectedCustomer = it }
        )
    }

    selectedCustomer?.let { customer ->
        CustomerDetailsDialog(
            customer = customer,
            isAdmin = currentSession.isAdmin,
            onDismiss = { selectedCustomer = null },
            onEdit = {
                selectedCustomer = null
                editingCustomer = customer
            },
            onRenew = {
                scope.launch {
                    loading = true
                    runCatching { api.renewCurrentMonth(currentSession.accessToken, customer.id) }
                        .onSuccess {
                            snackbarMessage = "تم تجديد اشتراك ${customer.fullName} للشهر الحالي."
                            selectedCustomer = null
                            refreshCustomers()
                        }
                        .onFailure { snackbarMessage = it.message ?: "تعذر تجديد الاشتراك." }
                    loading = false
                }
            }
        )
    }

    if (showAddDialog) {
        CustomerEditorDialog(
            title = "إضافة مشترك جديد",
            initial = null,
            showSubscribeOption = true,
            onDismiss = { showAddDialog = false },
            onSave = { draft, subscribeNow ->
                scope.launch {
                    loading = true
                    runCatching {
                        val saved = api.saveCustomer(currentSession.accessToken, draft)
                        if (subscribeNow) api.renewCurrentMonth(currentSession.accessToken, saved.id)
                    }.onSuccess {
                        showAddDialog = false
                        snackbarMessage = "تم حفظ المشترك بنجاح."
                        refreshCustomers()
                    }.onFailure { snackbarMessage = it.message ?: "تعذر حفظ المشترك." }
                    loading = false
                }
            }
        )
    }

    editingCustomer?.let { customer ->
        CustomerEditorDialog(
            title = "تعديل بيانات المشترك",
            initial = customer,
            showSubscribeOption = false,
            onDismiss = { editingCustomer = null },
            onSave = { draft, _ ->
                scope.launch {
                    loading = true
                    runCatching { api.saveCustomer(currentSession.accessToken, draft) }
                        .onSuccess {
                            editingCustomer = null
                            snackbarMessage = "تم تحديث البيانات."
                            refreshCustomers()
                        }
                        .onFailure { snackbarMessage = it.message ?: "تعذر تحديث البيانات." }
                    loading = false
                }
            }
        )
    }
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            EcoWasteIllustration(modifier = Modifier.fillMaxWidth().height(250.dp))
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إيكو ويست",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "إدارة ومتابعة اشتراكات جمع المخلفات",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("البريد الإلكتروني") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                    onClick = { onLogin(email, password) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("تسجيل الدخول", fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "الدخول متاح فقط للأدمن والمسؤول الميداني المصرح لهما.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EcoWasteIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFDDF7F0), Color(0xFFF7FBF7))),
            cornerRadius = CornerRadius(0f, 0f)
        )
        val groundY = size.height * 0.82f
        drawRect(Color(0xFFB9DE9B), topLeft = Offset(0f, groundY), size = Size(size.width, size.height - groundY))

        val truckX = size.width * 0.12f
        val truckY = size.height * 0.42f
        drawRoundRect(
            color = Color(0xFF2F8F4E),
            topLeft = Offset(truckX, truckY),
            size = Size(size.width * 0.48f, size.height * 0.28f),
            cornerRadius = CornerRadius(22f, 22f)
        )
        drawRoundRect(
            color = Color(0xFFE9F1EE),
            topLeft = Offset(size.width * 0.52f, size.height * 0.49f),
            size = Size(size.width * 0.18f, size.height * 0.21f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRect(
            color = Color(0xFF8FD0D8),
            topLeft = Offset(size.width * 0.56f, size.height * 0.52f),
            size = Size(size.width * 0.09f, size.height * 0.07f)
        )
        drawCircle(Color(0xFF26352F), radius = size.height * 0.055f, center = Offset(size.width * 0.27f, size.height * 0.73f))
        drawCircle(Color(0xFF26352F), radius = size.height * 0.055f, center = Offset(size.width * 0.59f, size.height * 0.73f))
        drawCircle(Color(0xFF7A8B84), radius = size.height * 0.025f, center = Offset(size.width * 0.27f, size.height * 0.73f))
        drawCircle(Color(0xFF7A8B84), radius = size.height * 0.025f, center = Offset(size.width * 0.59f, size.height * 0.73f))

        val binX = size.width * 0.76f
        drawRoundRect(Color(0xFF197B62), Offset(binX, size.height * 0.50f), Size(size.width * 0.12f, size.height * 0.24f), CornerRadius(12f, 12f))
        drawRect(Color(0xFF145C4B), Offset(binX - 8f, size.height * 0.48f), Size(size.width * 0.14f, size.height * 0.035f))
        drawCircle(Color(0xFF26352F), size.height * 0.018f, Offset(binX + size.width * 0.025f, size.height * 0.76f))
        drawCircle(Color(0xFF26352F), size.height * 0.018f, Offset(binX + size.width * 0.095f, size.height * 0.76f))

        val leafPath = Path().apply {
            moveTo(size.width * 0.79f, size.height * 0.20f)
            cubicTo(size.width * 0.86f, size.height * 0.09f, size.width * 0.94f, size.height * 0.13f, size.width * 0.92f, size.height * 0.28f)
            cubicTo(size.width * 0.86f, size.height * 0.33f, size.width * 0.81f, size.height * 0.29f, size.width * 0.79f, size.height * 0.20f)
        }
        drawPath(leafPath, Color(0xFF4EAD55))
        drawLine(Color(0xFF176B45), Offset(size.width * 0.78f, size.height * 0.31f), Offset(size.width * 0.90f, size.height * 0.16f), strokeWidth = 5f)

        drawCircle(Color.White.copy(alpha = 0.9f), radius = size.height * 0.06f, center = Offset(size.width * 0.36f, size.height * 0.56f), style = Stroke(width = 7f))
        drawLine(Color.White.copy(alpha = 0.9f), Offset(size.width * 0.33f, size.height * 0.56f), Offset(size.width * 0.39f, size.height * 0.56f), strokeWidth = 7f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EcoWasteTopBar(
    session: UserSession,
    loading: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("إيكو ويست", fontWeight = FontWeight.Black)
                Text(
                    text = if (session.isAdmin) "حساب الأدمن" else "المسؤول الميداني",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onLogout) {
                Icon(Icons.Rounded.Logout, contentDescription = "تسجيل الخروج")
            }
        },
        actions = {
            IconButton(enabled = !loading, onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "تحديث")
            }
        }
    )
}

@Composable
private fun CustomerSearchScreen(
    modifier: Modifier,
    session: UserSession,
    customers: List<CustomerStatus>,
    loading: Boolean,
    onCustomerClick: (CustomerStatus) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(customers, query) {
        val normalized = normalizeArabic(query)
        val digits = query.filter(Char::isDigit)
        if (query.isBlank()) customers.take(40)
        else customers.filter {
            normalizeArabic(it.fullName).contains(normalized) ||
                (!it.phone.isNullOrBlank() && digits.isNotBlank() && it.phone.filter(Char::isDigit).contains(digits))
        }.take(80)
    }
    val subscribed = customers.count { it.isSubscribedCurrentMonth }
    val periodLabel = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مرحبًا ${session.displayName}", fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("حالة الاشتراك محسوبة تلقائيًا لشهر $periodLabel")
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ابحث بالاسم أو رقم الهاتف") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, contentDescription = "مسح") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("المسجلون", customers.size.toString(), Modifier.weight(1f))
                StatCard("مشتركو الشهر", subscribed.toString(), Modifier.weight(1f))
                StatCard("غير مجدد", (customers.size - subscribed).toString(), Modifier.weight(1f))
            }
        }

        if (loading && customers.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                EmptySearchState(query)
            }
        } else {
            item {
                Text(
                    text = if (query.isBlank()) "أحدث قائمة المشتركين" else "نتائج البحث: ${filtered.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
            items(filtered, key = { it.id }) { customer ->
                CustomerCard(customer = customer, onClick = { onCustomerClick(customer) })
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 54.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔎", fontSize = 48.sp)
        Text(
            text = if (query.isBlank()) "لا توجد بيانات" else "لم يتم العثور على مشترك",
            fontWeight = FontWeight.Black
        )
        Text(
            text = if (query.isBlank()) "حدّث القائمة وحاول مرة أخرى" else "راجع الاسم أو رقم الهاتف",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomerCard(customer: CustomerStatus, onClick: () -> Unit) {
    val active = customer.isSubscribedCurrentMonth
    val container = if (active) Color(0xFFE7F6EC) else Color(0xFFFFECE7)
    val statusColor = if (active) Color(0xFF167644) else Color(0xFFB53A24)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.12f), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                        contentDescription = null,
                        tint = statusColor
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                customer.phone?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                customer.village?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = statusColor) {
                Text(
                    text = if (active) "مشترك" else "غير مشترك",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun CustomerDetailsDialog(
    customer: CustomerStatus,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRenew: () -> Unit
) {
    val active = customer.isSubscribedCurrentMonth
    val statusColor = if (active) Color(0xFF167644) else Color(0xFFB53A24)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(customer.fullName, fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Close, contentDescription = null, tint = statusColor)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(if (active) "الاشتراك نشط هذا الشهر" else "غير مشترك في الشهر الحالي", fontWeight = FontWeight.Black, color = statusColor)
                            Text(currentPeriodLabel(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                DetailRow(Icons.Rounded.Phone, "رقم الهاتف", customer.phone ?: "غير مسجل")
                DetailRow(Icons.Rounded.LocationOn, "القرية / المنطقة", customer.village ?: "غير مسجلة")
                DetailRow(Icons.Rounded.Person, "العنوان", customer.address ?: "غير مسجل")
                DetailRow(Icons.Rounded.CalendarMonth, "آخر اشتراك", lastSubscriptionLabel(customer))
                customer.notes?.let { DetailRow(Icons.Rounded.Edit, "ملاحظات", it) }
            }
        },
        confirmButton = {
            if (isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("تعديل")
                    }
                    if (!active) {
                        Button(onClick = onRenew) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("تجديد الشهر")
                        }
                    }
                }
            } else {
                TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        },
        dismissButton = {
            if (isAdmin) TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CustomerEditorDialog(
    title: String,
    initial: CustomerStatus?,
    showSubscribeOption: Boolean,
    onDismiss: () -> Unit,
    onSave: (CustomerDraft, Boolean) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.fullName.orEmpty()) }
    var phone by remember(initial?.id) { mutableStateOf(initial?.phone.orEmpty()) }
    var village by remember(initial?.id) { mutableStateOf(initial?.village.orEmpty()) }
    var address by remember(initial?.id) { mutableStateOf(initial?.address.orEmpty()) }
    var notes by remember(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }
    var subscribeNow by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("الاسم الكامل *") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter { char -> char.isDigit() || char == '+' } }, modifier = Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                OutlinedTextField(value = village, onValueChange = { village = it }, modifier = Modifier.fillMaxWidth(), label = { Text("القرية أو المنطقة") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text("العنوان التفصيلي") }, minLines = 2)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, modifier = Modifier.fillMaxWidth(), label = { Text("ملاحظات") }, minLines = 2)
                if (showSubscribeOption) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = subscribeNow, onCheckedChange = { subscribeNow = it })
                        Text("تسجيل اشتراك الشهر الحالي فورًا")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        CustomerDraft(
                            id = initial?.id,
                            fullName = name,
                            phone = phone.takeIf { it.isNotBlank() },
                            village = village.takeIf { it.isNotBlank() },
                            address = address.takeIf { it.isNotBlank() },
                            notes = notes.takeIf { it.isNotBlank() }
                        ),
                        subscribeNow
                    )
                }
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
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
    .replace(Regex("\\s+"), " ")

private fun currentPeriodLabel(): String =
    YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))

private fun lastSubscriptionLabel(customer: CustomerStatus): String {
    val year = customer.lastSubscriptionYear ?: return "لا يوجد اشتراك سابق"
    val month = customer.lastSubscriptionMonth ?: return "لا يوجد اشتراك سابق"
    val period = YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))
    return customer.lastPaidOn?.let { "$period — $it" } ?: period
}
