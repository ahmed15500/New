package com.ahmed.yawmeyaty

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

class EcoWasteModernActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YawmeyatyTheme {
                ModernEcoWasteApp()
            }
        }
    }
}

private enum class ModernScreen { HOME, SUBSCRIBERS }

private data class ModernSession(
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val role: String,
    val displayName: String,
    val phone: String
) {
    val isAdmin: Boolean get() = role == "admin"
}

private data class ModernCustomer(
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

private data class ModernCustomerDraft(
    val id: String? = null,
    val fullName: String,
    val phone: String?,
    val village: String?,
    val address: String?,
    val notes: String?
)

private data class NewAppUserDraft(
    val displayName: String,
    val phone: String,
    val password: String,
    val role: String
)

private class ModernSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)

    fun load(): ModernSession? {
        val token = prefs.getString("token", null) ?: return null
        return ModernSession(
            email = prefs.getString("email", "").orEmpty(),
            accessToken = token,
            refreshToken = prefs.getString("refresh", "").orEmpty(),
            role = prefs.getString("role", "field").orEmpty(),
            displayName = prefs.getString("name", "مستخدم إيكو ويست").orEmpty(),
            phone = prefs.getString("login_phone", "").orEmpty()
        )
    }

    fun save(session: ModernSession) {
        prefs.edit()
            .putString("email", session.email)
            .putString("token", session.accessToken)
            .putString("refresh", session.refreshToken)
            .putString("role", session.role)
            .putString("name", session.displayName)
            .putString("login_phone", session.phone)
            .apply()
    }

    fun clear() = prefs.edit().clear().apply()
}

private class ModernEcoWasteApi {
    suspend fun login(phoneInput: String, password: String): ModernSession = withContext(Dispatchers.IO) {
        val phone = normalizeEgyptianPhone(phoneInput)
        require(phone.matches(Regex("^01[0125][0-9]{8}$"))) {
            "اكتب رقم تليفون مصري صحيح مكوّن من 11 رقمًا."
        }

        val internalEmail = if (phone == PRIMARY_ADMIN_PHONE) {
            PRIMARY_ADMIN_INTERNAL_ID
        } else {
            "$phone@ecowaste.local"
        }

        val authRaw = request(
            method = "POST",
            path = "/auth/v1/token?grant_type=password",
            body = JSONObject()
                .put("email", internalEmail)
                .put("password", password)
                .toString()
        )
        val auth = JSONObject(authRaw)
        val token = auth.getString("access_token")
        val refresh = auth.optString("refresh_token")
        val userEmail = auth.getJSONObject("user").optString("email", internalEmail)

        val accessRaw = request(
            method = "GET",
            path = "/rest/v1/eco_waste_access?select=role,display_name&email=eq.${encode(userEmail)}",
            token = token
        )
        val rows = JSONArray(accessRaw)
        require(rows.length() > 0) { "هذا الرقم غير مصرح له باستخدام التطبيق." }
        val access = rows.getJSONObject(0)

        ModernSession(
            email = userEmail,
            accessToken = token,
            refreshToken = refresh,
            role = access.getString("role"),
            displayName = access.optString("display_name").ifBlank { phone },
            phone = phone
        )
    }

    suspend fun loadCustomers(token: String): List<ModernCustomer> = withContext(Dispatchers.IO) {
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
                    ModernCustomer(
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

    suspend fun saveCustomer(token: String, draft: ModernCustomerDraft): String = withContext(Dispatchers.IO) {
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


    suspend fun cancelCurrentMonth(token: String, customerId: String) = withContext(Dispatchers.IO) {
        val period = YearMonth.now()
        request(
            method = "DELETE",
            path = "/rest/v1/eco_waste_subscriptions" +
                "?customer_id=eq.${encode(customerId)}" +
                "&subscription_year=eq.${period.year}" +
                "&subscription_month=eq.${period.monthValue}",
            token = token,
            prefer = "return=minimal"
        )
    }

    suspend fun createUser(token: String, draft: NewAppUserDraft) = withContext(Dispatchers.IO) {
        val phone = normalizeEgyptianPhone(draft.phone)
        require(phone.matches(Regex("^01[0125][0-9]{8}$"))) { "رقم التليفون غير صحيح." }
        require(draft.displayName.isNotBlank()) { "اكتب اسم المستخدم." }
        require(draft.password.length >= 8) { "كلمة المرور يجب ألا تقل عن 8 أحرف." }

        val body = JSONObject()
            .put("display_name", draft.displayName.trim())
            .put("phone", phone)
            .put("password", draft.password)
            .put("role", draft.role)
            .toString()

        requestAbsolute(
            url = "$SUPABASE_URL/functions/v1/eco-waste-create-user",
            token = token,
            body = body
        )
    }

    private fun requestAbsolute(url: String, token: String, body: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20_000
        connection.readTimeout = 25_000
        connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (status !in 200..299) throw IllegalStateException(parseError(response, status))
        return response
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
        if (status !in 200..299) throw IllegalStateException(parseError(response, status))
        return response
    }

    private fun parseError(response: String, status: Int): String {
        val source = runCatching {
            val json = JSONObject(response)
            json.optString("error").ifBlank {
                json.optString("msg").ifBlank {
                    json.optString("message").ifBlank { json.optString("error_description") }
                }
            }
        }.getOrNull().orEmpty()

        return when {
            source.contains("Invalid login credentials", true) -> "رقم التليفون أو كلمة المرور غير صحيحة."
            source.contains("Email not confirmed", true) -> "الحساب لم يتم تفعيله بعد."
            source.contains("already", true) -> "يوجد حساب مسجل بهذا الرقم بالفعل."
            source == "admin_required" -> "إنشاء المستخدمين متاح للأدمن فقط."
            source == "invalid_phone" -> "رقم التليفون غير صحيح."
            source == "weak_password" -> "كلمة المرور يجب ألا تقل عن 8 أحرف."
            source.isNotBlank() -> source
            else -> "تعذر الاتصال بالخدمة. رمز الخطأ: $status"
        }
    }

    companion object {
        private const val PRIMARY_ADMIN_PHONE = "01208097044"
        private const val PRIMARY_ADMIN_INTERNAL_ID = "ahmedbahrawy814@gmail.com"
        private const val SUPABASE_URL = "https://pqevttogkdjyedljtyyd.supabase.co"
        private const val PUBLISHABLE_KEY = "sb_publishable_asDp-LKRawMRSaPIQi9i6w_dm0XTBvw"

        private fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}

@Composable
private fun ModernEcoWasteApp() {
    val context = LocalContext.current
    val store = remember { ModernSessionStore(context.applicationContext) }
    val api = remember { ModernEcoWasteApi() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var session by remember { mutableStateOf(store.load()) }
    var screen by remember {
        mutableStateOf(if (session?.isAdmin == false) ModernScreen.SUBSCRIBERS else ModernScreen.HOME)
    }
    var customers by remember { mutableStateOf(emptyList<ModernCustomer>()) }
    var loading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var selectedCustomer by remember { mutableStateOf<ModernCustomer?>(null) }
    var editingCustomer by remember { mutableStateOf<ModernCustomer?>(null) }
    var showAddCustomer by remember { mutableStateOf(false) }
    var showAddUser by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun logout() {
        store.clear()
        session = null
        customers = emptyList()
        screen = ModernScreen.HOME
    }

    fun refreshCustomers() {
        val current = session ?: return
        scope.launch {
            loading = true
            runCatching { api.loadCustomers(current.accessToken) }
                .onSuccess { customers = it }
                .onFailure {
                    message = it.message ?: "تعذر تحميل المشتركين."
                    if (it.message?.contains("JWT", true) == true) logout()
                }
            loading = false
        }
    }

    LaunchedEffect(screen, session?.accessToken) {
        if (session != null && screen == ModernScreen.SUBSCRIBERS && customers.isEmpty()) {
            refreshCustomers()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when {
            session == null -> ModernLoginScreen(
                modifier = Modifier.padding(padding),
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
                                screen = if (it.isAdmin) ModernScreen.HOME else ModernScreen.SUBSCRIBERS
                            }
                            .onFailure { loginError = it.message ?: "تعذر تسجيل الدخول." }
                        loading = false
                    }
                }
            )

            screen == ModernScreen.HOME -> ModernHomeScreen(
                modifier = Modifier.padding(padding),
                session = session!!,
                onOpenSubscribers = { screen = ModernScreen.SUBSCRIBERS },
                onAddUser = { showAddUser = true },
                onLogout = ::logout
            )

            else -> ModernSubscribersScreen(
                modifier = Modifier.padding(padding),
                session = session!!,
                customers = customers,
                loading = loading,
                onBack = {
                    if (session!!.isAdmin) screen = ModernScreen.HOME else Unit
                },
                onRefresh = ::refreshCustomers,
                onLogout = ::logout,
                onSelect = { selectedCustomer = it },
                onAdd = { showAddCustomer = true }
            )
        }
    }

    selectedCustomer?.let { customer ->
        ModernCustomerDetailsDialog(
            customer = customer,
            isAdmin = session?.isAdmin == true,
            onDismiss = { selectedCustomer = null },
            onEdit = {
                selectedCustomer = null
                editingCustomer = customer
            },
            onRenew = {
                val current = session ?: return@ModernCustomerDetailsDialog
                scope.launch {
                    loading = true
                    runCatching { api.renewCurrentMonth(current.accessToken, customer.id) }
                        .onSuccess {
                            selectedCustomer = null
                            message = "تم تجديد اشتراك ${customer.fullName} للشهر الحالي."
                            refreshCustomers()
                        }
                        .onFailure { message = it.message ?: "تعذر تجديد الاشتراك." }
                    loading = false
                }
            },
            onCancelRenewal = {
                val current = session ?: return@ModernCustomerDetailsDialog
                scope.launch {
                    loading = true
                    runCatching { api.cancelCurrentMonth(current.accessToken, customer.id) }
                        .onSuccess {
                            selectedCustomer = null
                            message = "تم إلغاء تجديد ${customer.fullName} للشهر الحالي وأصبح غير مشترك."
                            refreshCustomers()
                        }
                        .onFailure { message = it.message ?: "تعذر إلغاء التجديد." }
                    loading = false
                }
            }
        )
    }

    if (showAddCustomer) {
        ModernCustomerEditorDialog(
            title = "إضافة مشترك جديد",
            initial = null,
            showSubscribe = true,
            loading = loading,
            onDismiss = { if (!loading) showAddCustomer = false },
            onSave = { draft, subscribe ->
                val current = session ?: return@ModernCustomerEditorDialog
                scope.launch {
                    loading = true
                    runCatching {
                        val id = api.saveCustomer(current.accessToken, draft)
                        if (subscribe) api.renewCurrentMonth(current.accessToken, id)
                    }.onSuccess {
                        showAddCustomer = false
                        message = "تم حفظ المشترك بنجاح."
                        refreshCustomers()
                    }.onFailure { message = it.message ?: "تعذر حفظ المشترك." }
                    loading = false
                }
            }
        )
    }

    editingCustomer?.let { customer ->
        ModernCustomerEditorDialog(
            title = "تعديل بيانات المشترك",
            initial = customer,
            showSubscribe = false,
            loading = loading,
            onDismiss = { if (!loading) editingCustomer = null },
            onSave = { draft, _ ->
                val current = session ?: return@ModernCustomerEditorDialog
                scope.launch {
                    loading = true
                    runCatching { api.saveCustomer(current.accessToken, draft) }
                        .onSuccess {
                            editingCustomer = null
                            message = "تم تحديث بيانات المشترك."
                            refreshCustomers()
                        }
                        .onFailure { message = it.message ?: "تعذر تحديث البيانات." }
                    loading = false
                }
            }
        )
    }

    if (showAddUser) {
        ModernAddUserDialog(
            loading = loading,
            onDismiss = { if (!loading) showAddUser = false },
            onCreate = { draft ->
                val current = session ?: return@ModernAddUserDialog
                scope.launch {
                    loading = true
                    runCatching { api.createUser(current.accessToken, draft) }
                        .onSuccess {
                            showAddUser = false
                            message = "تم إنشاء حساب ${draft.displayName} بنجاح."
                        }
                        .onFailure { message = it.message ?: "تعذر إنشاء الحساب." }
                    loading = false
                }
            }
        )
    }
}

@Composable
private fun BrandLogo(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(14.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_ecowaste_brand),
            contentDescription = "شعار إيكو ويست",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ModernLoginScreen(
    modifier: Modifier,
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFE9F8F1),
                        MaterialTheme.colorScheme.background,
                        Color(0xFFFDF8EC)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrandLogo(Modifier.size(112.dp))
            Spacer(Modifier.height(18.dp))
            Text(
                "إيكو ويست",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                "إدارة ذكية لاشتراكات جمع المخلفات",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(26.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("تسجيل الدخول", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "استخدم رقم التليفون وكلمة المرور",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { char -> char.isDigit() || char == '+' }.take(15) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رقم التليفون") },
                        placeholder = { Text("مثال: 01XXXXXXXXX") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true
                    )
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
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true
                    )

                    error?.let {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                it,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Button(
                        enabled = !loading && phone.filter(Char::isDigit).length >= 10 && password.isNotBlank(),
                        onClick = { onLogin(phone, password) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        } else {
                            Text("تسجيل الدخول", fontWeight = FontWeight.Black, fontSize = 17.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "بياناتك مشفّرة ومحفوظة بأمان",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModernHomeScreen(
    modifier: Modifier,
    session: ModernSession,
    onOpenSubscribers: () -> Unit,
    onAddUser: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF063C3B), Color(0xFF0B6A58), Color(0xFF0C8A61))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandLogo(Modifier.size(76.dp))
                        Spacer(Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مرحبًا،", color = Color(0xFFBDF5D8))
                            Text(
                                session.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                if (session.isAdmin) "مدير النظام" else "مسؤول ميداني",
                                color = Color(0xFFF6D58A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("لوحة التحكم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }

        item {
            ModernActionCard(
                icon = Icons.Rounded.People,
                title = "المشتركين والاشتراكات",
                description = "ابحث بالاسم أو رقم التليفون وتابع حالة اشتراك الشهر الحالي.",
                buttonText = "فتح سجل المشتركين",
                accent = Color(0xFF087A5B),
                onClick = onOpenSubscribers
            )
        }

        if (session.isAdmin) {
            item {
                ModernActionCard(
                    icon = Icons.Rounded.PersonAdd,
                    title = "إدارة مستخدمي التطبيق",
                    description = "أضف أدمن أو مسؤولًا ميدانيًا برقم التليفون وكلمة المرور.",
                    buttonText = "إضافة مستخدم جديد",
                    accent = Color(0xFFC08B24),
                    onClick = onAddUser
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(Icons.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ModernActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.12f)) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp).size(28.dp), tint = accent)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(buttonText, fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernSubscribersScreen(
    modifier: Modifier,
    session: ModernSession,
    customers: List<ModernCustomer>,
    loading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onSelect: (ModernCustomer) -> Unit,
    onAdd: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val nameQuery = normalizeSearch(query)
    val phoneQuery = normalizeDigits(query)
    val filtered = remember(customers, nameQuery, phoneQuery) {
        if (nameQuery.isBlank() && phoneQuery.isBlank()) customers
        else customers.filter { customer ->
            val matchesName = nameQuery.isNotBlank() && normalizeSearch(customer.fullName).contains(nameQuery)
            val matchesPhone = phoneQuery.isNotBlank() && normalizeDigits(customer.phone.orEmpty()).contains(phoneQuery)
            matchesName || matchesPhone
        }
    }
    val activeCount = customers.count { it.activeThisMonth }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سجل المشتركين", fontWeight = FontWeight.Black)
                        Text(
                            "${session.displayName} • ${if (session.isAdmin) "مدير" else "مسؤول ميداني"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (session.isAdmin) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "رجوع")
                        }
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
        },
        floatingActionButton = {
            if (session.isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("إضافة مشترك", fontWeight = FontWeight.Black) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(Color(0xFF063C3B), Color(0xFF0B765C))))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandLogo(Modifier.size(62.dp))
                            Spacer(Modifier.size(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("اشتراكات ${currentMonthLabel()}", color = Color.White, fontWeight = FontWeight.Black)
                                Text("إجمالي الأسماء: ${customers.size}", color = Color(0xFFCEF4E2))
                                Text("مشترك حاليًا: $activeCount", color = Color(0xFFF6D58A), fontWeight = FontWeight.Bold)
                            }
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
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )
            }

            if (query.isNotBlank()) {
                item {
                    Text(
                        "عدد النتائج: ${filtered.size}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (loading && customers.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(42.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }
            } else if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("لا توجد نتائج مطابقة", fontWeight = FontWeight.Black)
                            Text("جرّب كتابة جزء من الاسم أو رقم التليفون")
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { customer ->
                    ModernCustomerCard(customer = customer, onClick = { onSelect(customer) })
                }
            }
        }
    }
}

@Composable
private fun ModernCustomerCard(customer: ModernCustomer, onClick: () -> Unit) {
    val active = customer.activeThisMonth
    val accent = if (active) Color(0xFF087A5B) else Color(0xFFBA1A1A)
    val container = if (active) Color(0xFFE8F8F0) else Color(0xFFFFEEEC)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = accent) {
                Icon(
                    if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(customer.fullName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(customer.phone ?: "لا يوجد رقم تليفون", color = MaterialTheme.colorScheme.onSurfaceVariant)
                customer.village?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (active) "مشترك في ${currentMonthLabel()}" else "غير مشترك في الشهر الحالي",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ModernCustomerDetailsDialog(
    customer: ModernCustomer,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRenew: () -> Unit,
    onCancelRenewal: () -> Unit
) {
    var confirmCancel by remember(customer.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (customer.activeThisMonth) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                contentDescription = null,
                tint = if (customer.activeThisMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        title = { Text(customer.fullName, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusSurface(customer.activeThisMonth)
                HorizontalDivider()
                Text("رقم التليفون: ${customer.phone ?: "غير مسجل"}")
                Text("القرية/المنطقة: ${customer.village ?: "غير مسجلة"}")
                customer.address?.let { Text("العنوان: $it") }
                Text("آخر اشتراك: ${lastPeriodLabel(customer)}")
                customer.notes?.let { Text("ملاحظات: $it") }
            }
        },
        confirmButton = {
            when {
                isAdmin && customer.activeThisMonth -> {
                    OutlinedButton(
                        onClick = { confirmCancel = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("إلغاء تجديد الشهر")
                    }
                }
                isAdmin -> {
                    Button(onClick = onRenew, shape = RoundedCornerShape(14.dp)) {
                        Text("تجديد الشهر الحالي")
                    }
                }
                else -> TextButton(onClick = onDismiss) { Text("إغلاق") }
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("إغلاق") }
                if (isAdmin) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("تعديل")
                    }
                }
            }
        }
    )

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            icon = { Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("إلغاء تجديد الاشتراك؟", fontWeight = FontWeight.Black) },
            text = { Text("سيصبح ${customer.fullName} غير مشترك في الشهر الحالي. لن يتم حذف بيانات المشترك.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmCancel = false
                        onCancelRenewal()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("نعم، إلغاء التجديد") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text("رجوع") }
            }
        )
    }
}

@Composable
private fun StatusSurface(active: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (active) Color(0xFFE1F6EB) else MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = if (active) "مشترك في الشهر الحالي" else "غير مشترك في الشهر الحالي",
            modifier = Modifier.padding(12.dp),
            fontWeight = FontWeight.Black,
            color = if (active) Color(0xFF087A5B) else MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ModernCustomerEditorDialog(
    title: String,
    initial: ModernCustomer?,
    showSubscribe: Boolean,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSave: (ModernCustomerDraft, Boolean) -> Unit
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
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = normalizeDigits(it).take(11) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رقم التليفون") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = village,
                    onValueChange = { village = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("القرية أو المنطقة") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("العنوان") },
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظات") },
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2
                )
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
                enabled = !loading && name.isNotBlank(),
                onClick = {
                    onSave(
                        ModernCustomerDraft(
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
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("إلغاء") } }
    )
}

@Composable
private fun ModernAddUserDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (NewAppUserDraft) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("field") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مستخدم جديد", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("الاسم") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = normalizeDigits(it).take(11) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رقم التليفون") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("كلمة المرور") },
                    supportingText = { Text("8 أحرف على الأقل") },
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
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                Text("الصلاحية", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == "field",
                        onClick = { role = "field" },
                        label = { Text("مسؤول ميداني") }
                    )
                    FilterChip(
                        selected = role == "admin",
                        onClick = { role = "admin" },
                        label = { Text("أدمن") }
                    )
                }
                Text(
                    "كلمة المرور تُرسل مباشرة إلى Supabase Auth ولا تُحفظ داخل التطبيق.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !loading && name.isNotBlank() && phone.length == 11 && password.length >= 8,
                onClick = {
                    onCreate(
                        NewAppUserDraft(
                            displayName = name,
                            phone = phone,
                            password = password,
                            role = role
                        )
                    )
                }
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("إنشاء الحساب")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("إلغاء") } }
    )
}

private fun normalizeEgyptianPhone(value: String): String {
    val digits = normalizeDigits(value)
    return when {
        digits.startsWith("0020") && digits.length == 14 -> "0${digits.drop(4)}"
        digits.startsWith("20") && digits.length == 12 -> "0${digits.drop(2)}"
        digits.length == 10 && digits.startsWith("1") -> "0$digits"
        else -> digits
    }
}

private fun normalizeDigits(value: String): String = buildString {
    value.forEach { char ->
        when (char) {
            '٠', '۰' -> append('0')
            '١', '۱' -> append('1')
            '٢', '۲' -> append('2')
            '٣', '۳' -> append('3')
            '٤', '۴' -> append('4')
            '٥', '۵' -> append('5')
            '٦', '۶' -> append('6')
            '٧', '۷' -> append('7')
            '٨', '۸' -> append('8')
            '٩', '۹' -> append('9')
            in '0'..'9' -> append(char)
        }
    }
}

private fun normalizeSearch(value: String): String = value
    .trim()
    .lowercase(Locale("ar"))
    .replace(Regex("[ًٌٍَُِّْٰ]"), "")
    .replace('أ', 'ا')
    .replace('إ', 'ا')
    .replace('آ', 'ا')
    .replace('ى', 'ي')
    .replace('ة', 'ه')
    .replace(Regex(" +"), " ")

private fun JSONObject.nullableString(key: String): String? =
    if (isNull(key)) null else optString(key).trim().takeIf { it.isNotEmpty() }

private fun JSONObject.nullableInt(key: String): Int? =
    if (isNull(key)) null else optInt(key)

private fun JSONObject.putNullable(key: String, value: String?): JSONObject =
    if (value.isNullOrBlank()) put(key, JSONObject.NULL) else put(key, value.trim())

private fun currentMonthLabel(): String =
    YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))

private fun lastPeriodLabel(customer: ModernCustomer): String {
    val year = customer.lastYear ?: return "لا يوجد اشتراك سابق"
    val month = customer.lastMonth ?: return "لا يوجد اشتراك سابق"
    val period = YearMonth.of(year, month)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "EG")))
    return customer.lastPaidOn?.let { "$period — $it" } ?: period
}

private const val SESSION_PREFS = "eco_waste_phone_session"