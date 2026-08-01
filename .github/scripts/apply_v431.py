from pathlib import Path
import re

path = Path("app/src/main/java/com/ahmed/yawmeyaty/EcoWasteModernActivity.kt")
text = path.read_text(encoding="utf-8")

cancel_method = '''
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

'''

if "suspend fun cancelCurrentMonth" not in text:
    marker = "    suspend fun createUser(token: String, draft: NewAppUserDraft)"
    if marker not in text:
        raise SystemExit("createUser marker not found")
    text = text.replace(marker, cancel_method + marker, 1)

selected_block = '''    selectedCustomer?.let { customer ->
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

'''

pattern = re.compile(r"    selectedCustomer\?\.let \{ customer ->.*?\n    \}\n\n    if \(showAddCustomer\)", re.S)
if not pattern.search(text):
    raise SystemExit("selected customer block not found")
text = pattern.sub(selected_block + "    if (showAddCustomer)", text, count=1)

details_function = '''@Composable
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

'''

pattern = re.compile(r"@Composable\nprivate fun ModernCustomerDetailsDialog\(.*?\n\}\n\n@Composable\nprivate fun StatusSurface", re.S)
if not pattern.search(text):
    raise SystemExit("details dialog function not found")
text = pattern.sub(details_function + "@Composable\nprivate fun StatusSurface", text, count=1)

path.write_text(text, encoding="utf-8")
print("Applied Eco Waste v4.3.1 source changes")
