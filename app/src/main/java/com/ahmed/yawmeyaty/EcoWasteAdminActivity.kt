package com.ahmed.yawmeyaty

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahmed.yawmeyaty.ui.theme.YawmeyatyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class EcoWasteAdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YawmeyatyTheme {
                AdminHome(
                    openSubscribers = { startActivity(Intent(this, EcoWastePhoneActivity::class.java)) },
                    openLogin = { startActivity(Intent(this, EcoWastePhoneActivity::class.java)) }
                )
            }
        }
    }
}

private data class StoredAccess(val token: String, val role: String, val name: String)

private fun readAccess(context: Context): StoredAccess? {
    val prefs = context.getSharedPreferences("eco_waste_phone_session", Context.MODE_PRIVATE)
    val token = prefs.getString("token", null) ?: return null
    return StoredAccess(
        token = token,
        role = prefs.getString("role", "field").orEmpty(),
        name = prefs.getString("name", "مستخدم إيكو ويست").orEmpty()
    )
}

private suspend fun createEcoWasteUser(
    token: String,
    name: String,
    phone: String,
    password: String,
    role: String
) = withContext(Dispatchers.IO) {
    val body = JSONObject()
        .put("display_name", name.trim())
        .put("phone", phone.filter(Char::isDigit))
        .put("password", password)
        .put("role", role)
        .toString()

    val connection = URL("https://pqevttogkdjyedljtyyd.supabase.co/functions/v1/eco-waste-create-user")
        .openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.connectTimeout = 20_000
    connection.readTimeout = 25_000
    connection.setRequestProperty("Authorization", "Bearer $token")
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
    connection.doOutput = true
    connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

    val status = connection.responseCode
    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
    val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
    connection.disconnect()

    if (status !in 200..299) {
        val source = runCatching { JSONObject(response).optString("error") }.getOrNull().orEmpty()
        val message = when {
            source.contains("already", true) -> "يوجد حساب مسجل بهذا الرقم بالفعل."
            source == "admin_required" -> "إنشاء الحسابات متاح للأدمن فقط."
            source == "invalid_phone" -> "رقم التليفون غير صحيح."
            source == "weak_password" -> "كلمة المرور يجب ألا تقل عن 8 أحرف."
            source.isNotBlank() -> source
            else -> "تعذر إنشاء الحساب. رمز الخطأ: $status"
        }
        throw IllegalStateException(message)
    }
}

@Composable
private fun AdminHome(openSubscribers: () -> Unit, openLogin: () -> Unit) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(readAccess(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("إيكو ويست", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

            val current = access
            if (current == null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("سجّل الدخول أولًا", fontWeight = FontWeight.Black)
                        Text("استخدم رقم التليفون وكلمة المرور، ثم ارجع إلى هذه الشاشة.")
                        Button(onClick = openLogin, modifier = Modifier.fillMaxWidth()) { Text("فتح شاشة الدخول") }
                        OutlinedButton(
                            onClick = { access = readAccess(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تم تسجيل الدخول — تحديث") }
                    }
                }
            } else {
                Text("مرحبًا، ${current.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.People, contentDescription = null, modifier = Modifier.size(34.dp))
                        Text("المشتركين والاشتراكات", fontWeight = FontWeight.Black)
                        Button(onClick = openSubscribers, modifier = Modifier.fillMaxWidth()) {
                            Text("فتح سجل المشتركين")
                        }
                    }
                }

                if (current.role == "admin") {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(34.dp))
                            Text("إدارة مستخدمي التطبيق", fontWeight = FontWeight.Black)
                            Text("إنشاء حساب أدمن أو مسؤول ميداني برقم الهاتف وكلمة المرور.")
                            Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("إضافة مستخدم جديد")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog && access != null) {
        AddUserDialog(
            loading = loading,
            onDismiss = { if (!loading) showDialog = false },
            onCreate = { name, phone, password, role ->
                scope.launch {
                    loading = true
                    runCatching { createEcoWasteUser(access!!.token, name, phone, password, role) }
                        .onSuccess {
                            showDialog = false
                            snackbar.showSnackbar("تم إنشاء حساب $name بنجاح.")
                        }
                        .onFailure { snackbar.showSnackbar(it.message ?: "تعذر إنشاء الحساب.") }
                    loading = false
                }
            }
        )
    }
}

@Composable
private fun AddUserDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

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
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { char -> char.isDigit() }.take(11) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("رقم التليفون") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAdmin, onCheckedChange = { isAdmin = it })
                    Text(if (isAdmin) "أدمن" else "مسؤول ميداني")
                }
                Text(
                    "كلمة المرور تُرسل مباشرة إلى Supabase Auth ولا تُحفظ داخل التطبيق.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !loading && name.isNotBlank() && phone.length == 11 && password.length >= 8,
                onClick = { onCreate(name, phone, password, if (isAdmin) "admin" else "field") }
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("إنشاء الحساب")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !loading) { Text("إلغاء") } }
    )
}