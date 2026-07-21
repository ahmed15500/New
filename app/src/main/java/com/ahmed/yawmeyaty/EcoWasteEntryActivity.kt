package com.ahmed.yawmeyaty

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class EcoWasteEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (hasSavedSession()) {
            openDashboard()
            return
        }

        setContent {
            YawmeyatyTheme {
                EntryLoginScreen(
                    onSuccess = { openDashboard() }
                )
            }
        }
    }

    private fun hasSavedSession(): Boolean =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("token", null)
            .isNullOrBlank()
            .not()

    private fun openDashboard() {
        startActivity(Intent(this, EcoWasteAdminActivity::class.java))
        finish()
    }

    companion object {
        private const val PREFS = "eco_waste_phone_session"
    }
}

private data class EntrySession(
    val email: String,
    val token: String,
    val refresh: String,
    val role: String,
    val name: String
)

private class EntryApi(private val context: Context) {
    suspend fun login(phoneInput: String, password: String): EntrySession = withContext(Dispatchers.IO) {
        val phone = normalizeEntryPhone(phoneInput)
        require(phone.matches(Regex("^01[0125]\\d{8}$"))) {
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

        val session = EntrySession(
            email = userEmail,
            token = token,
            refresh = refresh,
            role = access.getString("role"),
            name = access.optString("display_name").ifBlank { phone }
        )
        saveSession(session)
        session
    }

    private fun saveSession(session: EntrySession) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("email", session.email)
            .putString("token", session.token)
            .putString("refresh", session.refresh)
            .putString("role", session.role)
            .putString("name", session.name)
            .putString("login_phone", PRIMARY_ADMIN_PHONE)
            .apply()
    }

    private fun request(
        method: String,
        path: String,
        token: String? = null,
        body: String? = null
    ): String {
        val connection = URL(SUPABASE_URL + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 20_000
        connection.readTimeout = 25_000
        connection.setRequestProperty("apikey", PUBLISHABLE_KEY)
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }

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
                json.optString("msg").ifBlank {
                    json.optString("message").ifBlank { json.optString("error_description") }
                }
            }.getOrNull().orEmpty()
            val message = when {
                source.contains("Invalid login credentials", true) -> "رقم التليفون أو كلمة المرور غير صحيحة."
                source.contains("Email not confirmed", true) -> "الحساب لم يتم تفعيله بعد."
                source.isNotBlank() -> source
                else -> "تعذر الاتصال بالخدمة. رمز الخطأ: $status"
            }
            throw IllegalStateException(message)
        }
        return response
    }

    companion object {
        private const val PREFS = "eco_waste_phone_session"
        private const val PRIMARY_ADMIN_PHONE = "01208097044"
        private const val PRIMARY_ADMIN_INTERNAL_ID = "ahmedbahrawy814@gmail.com"
        private const val SUPABASE_URL = "https://pqevttogkdjyedljtyyd.supabase.co"
        private const val PUBLISHABLE_KEY = "sb_publishable_asDp-LKRawMRSaPIQi9i6w_dm0XTBvw"

        private fun encode(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }
}

private fun normalizeEntryPhone(value: String): String {
    val digits = value.filter(Char::isDigit)
    return when {
        digits.startsWith("0020") && digits.length == 14 -> "0${digits.drop(4)}"
        digits.startsWith("20") && digits.length == 12 -> "0${digits.drop(2)}"
        digits.length == 10 && digits.startsWith("1") -> "0$digits"
        else -> digits
    }
}

@Composable
private fun EntryLoginScreen(onSuccess: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val api = remember { EntryApi(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
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
            text = "تسجيل الدخول برقم التليفون",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { char -> char.isDigit() || char == '+' }.take(15) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("رقم التليفون") },
            placeholder = { Text("مثال: 01208097044") },
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
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    runCatching { api.login(phone, password) }
                        .onSuccess { onSuccess() }
                        .onFailure { error = it.message ?: "تعذر تسجيل الدخول." }
                    loading = false
                }
            },
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
            text = "رقم الأدمن الرئيسي: 01208097044",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}