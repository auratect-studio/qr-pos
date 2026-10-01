package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCardDownload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Print
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.QrPaymentMode
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.network.webhook.StreamConnectionState
import com.example.ui.components.BluetoothPrinterDialog
import com.example.ui.components.TipsSelectorRow
import com.example.util.ReceiptGenerator
import com.example.viewmodel.ActivePayment
import java.util.Locale

@Composable
fun QrPaymentDialog(
    activePayment: ActivePayment,
    isPremium: Boolean,
    taxNumber: String,
    onDismiss: () -> Unit,
    onSimulatePayment: (Context) -> Unit,
    onOpenSubscription: () -> Unit = {},
    merchant: com.example.model.MerchantProfile? = null,
    onCheckMonoPayment: ((Context) -> Unit)? = null,
    isCheckingMono: Boolean = false,
    monoCooldownSeconds: Int = 0,
    onSelectMode: (QrPaymentMode) -> Unit = {},
    onFiscalizeCheckbox: ((TransactionRecord) -> Unit)? = null,
    isFiscalizingCheckbox: Boolean = false,
    lastFiscalCode: String? = null,
    streamConnectionState: StreamConnectionState = StreamConnectionState.DISCONNECTED,
    onTriggerTestWebhook: (() -> Unit)? = null,
    onSelectTip: (Double) -> Unit = {},
    onOpenCustomerDisplay: () -> Unit = {}
) {
    val context = LocalContext.current
    var showReceiptPreview by remember { mutableStateOf(false) }
    var showProReceiptLockedDialog by remember { mutableStateOf(false) }
    var showBluetoothPrinterDialog by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (activePayment.isPaid) "ОПЛАЧЕНО" else "СКАНУЙТЕ ДЛЯ ОПЛАТИ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (activePayment.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Динамічний банківський QR-код",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрити",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Live Webhook channel indicator badge
                if (!activePayment.isPaid) {
                    val (statusDot, statusText) = when (streamConnectionState) {
                        StreamConnectionState.CONNECTED -> "🟢" to "Live Webhook активний (миттєве зарахування)"
                        StreamConnectionState.CONNECTING -> "🟡" to "Підключення до Live Webhook шлюзу..."
                        else -> "⚪" to "Live Webhook: офлайн (перевірка за запитом)"
                    }
                    val badgeBg = when (streamConnectionState) {
                        StreamConnectionState.CONNECTED -> Color(0xFF064E3B).copy(alpha = 0.35f)
                        StreamConnectionState.CONNECTING -> Color(0xFF78350F).copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                    val textColor = when (streamConnectionState) {
                        StreamConnectionState.CONNECTED -> Color(0xFF34D399)
                        StreamConnectionState.CONNECTING -> Color(0xFFFBBF24)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = statusDot, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2. Тумблер вибору формату QR-коду (Камера смартфону vs Сканер банку)
                if (!activePayment.isPaid) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        QrPaymentMode.entries.forEach { mode ->
                            val isSelected = activePayment.currentMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { onSelectMode(mode) }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = mode.iconEmoji, fontSize = 14.sp)
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 3. QR Code Container with Diia styling
                val displayQrBitmap = activePayment.currentQrBitmap
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(
                            width = 3.dp,
                            color = if (activePayment.isPaid) MaterialTheme.colorScheme.primary
                            else Color(activePayment.bank.brandColorHex),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayQrBitmap != null) {
                        Image(
                            bitmap = displayQrBitmap.asImageBitmap(),
                            contentDescription = "Динамічний платіжний QR-код",
                            modifier = Modifier.size(210.dp)
                        )
                    }

                    if (activePayment.isPaid) {
                        // Success overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color(0xEE064E3B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ОПЛАЧЕНО",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Підказка для обраного режиму сканування
                if (!activePayment.isPaid) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = activePayment.currentMode.instruction,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Tips selector module
                if (!activePayment.isPaid) {
                    TipsSelectorRow(
                        baseAmount = activePayment.amount,
                        selectedTipAmount = activePayment.tipsAmount,
                        isPremium = isPremium,
                        onTipSelected = onSelectTip
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Amount banner
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", activePayment.totalAmount)} ₴",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (activePayment.tipsAmount > 0.0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Разом: замовлення ${String.format(Locale.US, "%.2f", activePayment.amount)} ₴ + чайові ${String.format(Locale.US, "%.2f", activePayment.tipsAmount)} ₴",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!isPremium && merchant != null) {
                        val baseFee = activePayment.amount * 0.008
                        val tipsFee = activePayment.tipsAmount * 0.005
                        val totalFee = baseFee + tipsFee
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Комісія: 0.8% (${String.format(Locale.US, "%.2f", totalFee)} ₴) • Баланс: ${String.format(Locale.US, "%.2f", merchant.serviceBalance)} ₴",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (merchant.serviceBalance <= 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─── Головні дії касира ───────────────────────────
                if (!activePayment.isPaid) {
                    // 1. Кнопка підтвердження оплати касиром (миттєве зарахування)
                    Button(
                        onClick = { onSimulatePayment(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_payment_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "✅ Підтвердити оплату (Касир)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Відкрити дисплей покупця на весь екран
                    OutlinedButton(
                        onClick = {
                            if (isPremium) {
                                onOpenCustomerDisplay()
                            } else {
                                onOpenSubscription()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isPremium) Color(0xFF0D9488) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPremium) "🖥️ Відкрити Дисплей покупця" else "🖥️ Дисплей покупця (PRO 👑)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Перемикач інструментів діагностики та реквізитів
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { showDiagnostics = !showDiagnostics }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showDiagnostics) "▲ Приховати інструменти діагностики" else "🛠️ Додаткові інструменти та діагностика ▼",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showDiagnostics) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Requisite details card (Bank, Account/IBAN, Amount)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RequisiteRow(
                                        title = "Банкінг",
                                        value = activePayment.bank.displayName,
                                        color = Color(activePayment.bank.brandColorHex)
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                    RequisiteRow(
                                        title = "Одержувач",
                                        value = activePayment.recipientName
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Номер рахунку (IBAN)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = activePayment.iban,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable {
                                                    val clipboard =
                                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(
                                                        ClipData.newPlainText("IBAN", activePayment.iban)
                                                    )
                                                    Toast.makeText(context, "IBAN скопійовано!", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Копіювати IBAN",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                    RequisiteRow(
                                        title = "Призначення",
                                        value = activePayment.purpose
                                    )
                                }
                            }

                            // Відкрити банківський додаток / Web-міст
                            val bank = activePayment.bank
                            OutlinedButton(
                                onClick = {
                                    try {
                                        if (activePayment.currentMode == QrPaymentMode.WEB_LINK) {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(activePayment.bankPaymentUrl))
                                            )
                                        } else {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(bank.packageName)
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                            } else {
                                                val (deepLink, _) = com.example.util.QrGenerator.buildBankOpenIntents(bank)
                                                val deepIntent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                                                val resolved = deepIntent.resolveActivity(context.packageManager)
                                                if (resolved != null) {
                                                    context.startActivity(deepIntent)
                                                } else {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activePayment.bankPaymentUrl)))
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Неможливо відкрити ${bank.shortName}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("test_client_link_btn"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = if (activePayment.currentMode == QrPaymentMode.WEB_LINK) Icons.Default.OpenInNew else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (activePayment.currentMode == QrPaymentMode.WEB_LINK) "Відкрити Web-міст оплати"
                                    else "Відкрити ${bank.shortName} на терміналі"
                                )
                            }

                            // Поділитися лінком
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val shareText = buildString {
                                            append("💳 Рахунок на оплату від ${activePayment.recipientName.ifBlank { "QR POS" }}\n")
                                            append("Сума: ${String.format(java.util.Locale.US, "%.2f", activePayment.totalAmount)} ₴\n")
                                            append("IBAN: ${activePayment.iban}\n")
                                            append("Призначення: ${activePayment.purpose}\n\n")
                                            append("🔗 Посилання для оплати: ${activePayment.bankPaymentUrl}")
                                        }
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Надіслати реквізити клієнту"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Не вдалося відкрити діалог надсилання", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Поділитися реквізитами та лінком")
                            }

                            // Жива перевірка Monobank API
                            if (onCheckMonoPayment != null) {
                                val isCooldownActive = monoCooldownSeconds > 0
                                val isMonoBtnDisabled = isCheckingMono || isCooldownActive

                                Button(
                                    onClick = { onCheckMonoPayment(context) },
                                    enabled = !isMonoBtnDisabled,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("check_mono_live_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCooldownActive) Color(0xFF334155) else Color(0xFF1E293B),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFF334155),
                                        disabledContentColor = Color(0xFF94A3B8)
                                    )
                                ) {
                                    if (isCheckingMono) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Зв'язок з Monobank API...", fontWeight = FontWeight.Bold)
                                    } else if (isCooldownActive) {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("⏳ Захист від повтору (${monoCooldownSeconds}с)", fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Жива перевірка (Monobank API)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Тест Edge Webhook
                            if (onTriggerTestWebhook != null) {
                                OutlinedButton(
                                    onClick = { onTriggerTestWebhook() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("simulate_webhook_btn"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF10B981)
                                    )
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("⚡ Тест Edge Webhook (імітація)", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    // Checkbox PRRO Fiscalization button
                    if (onFiscalizeCheckbox != null) {
                        val recordForFiscal = TransactionRecord(
                            id = activePayment.id,
                            amount = activePayment.amount,
                            feeAmount = 0.0,
                            netAmount = activePayment.amount,
                            bank = activePayment.bank,
                            recipientName = activePayment.recipientName,
                            iban = activePayment.iban,
                            purpose = activePayment.purpose,
                            itemsSummary = activePayment.purpose,
                            timestamp = System.currentTimeMillis(),
                            status = TransactionStatus.PAID
                        )

                        Button(
                            onClick = { onFiscalizeCheckbox(recordForFiscal) },
                            enabled = !isFiscalizingCheckbox,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fiscalize_checkbox_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lastFiscalCode != null) Color(0xFF047857) else Color(0xFF0284C7),
                                contentColor = Color.White
                            )
                        ) {
                            if (isFiscalizingCheckbox) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Фіскалізація в ДПС (Checkbox)...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (lastFiscalCode != null) "✅ Фіскалізовано в ДПС №$lastFiscalCode"
                                    else "Фіскалізувати в ДПС (Checkbox ПРРО)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Bluetooth Thermal Print Button (ESC/POS)
                    val paidRecord = TransactionRecord(
                        id = activePayment.id,
                        amount = activePayment.amount,
                        feeAmount = 0.0,
                        netAmount = activePayment.amount,
                        bank = activePayment.bank,
                        recipientName = activePayment.recipientName,
                        iban = activePayment.iban,
                        purpose = activePayment.purpose,
                        itemsSummary = activePayment.purpose,
                        timestamp = System.currentTimeMillis(),
                        status = TransactionStatus.PAID
                    )

                    Button(
                        onClick = { showBluetoothPrinterDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bluetooth_print_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🖨️ Друкувати чек (Bluetooth ESC/POS)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action 3: Generate and Share Electronic Text Receipt (Paid requirement 4)
                    if (!isPremium) {
                        Button(
                            onClick = { showProReceiptLockedDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_receipt_btn_locked"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сформувати та надіслати чек (PRO)", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val dummyRecord = TransactionRecord(
                                    id = activePayment.id,
                                    amount = activePayment.amount,
                                    feeAmount = 0.0,
                                    netAmount = activePayment.amount,
                                    bank = activePayment.bank,
                                    recipientName = activePayment.recipientName,
                                    iban = activePayment.iban,
                                    purpose = activePayment.purpose,
                                    itemsSummary = activePayment.purpose,
                                    timestamp = System.currentTimeMillis(),
                                    status = TransactionStatus.PAID
                                )
                                val receipt = ReceiptGenerator.generateReceiptText(
                                    transaction = dummyRecord,
                                    cartItems = activePayment.cartSnapshot,
                                    taxNumber = taxNumber,
                                    merchant = merchant
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Електронний чек №${activePayment.id.takeLast(6)}")
                                    putExtra(Intent.EXTRA_TEXT, receipt)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Надіслати чек клієнту (SMS/Email/Viber)"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_receipt_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Надіслати чек клієнту (SMS/Email)", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { showReceiptPreview = !showReceiptPreview },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (showReceiptPreview) "Приховати текст чека" else "Переглянути текст чека")
                        }

                        AnimatedVisibility(visible = showReceiptPreview) {
                            val dummyRecord = TransactionRecord(
                                id = activePayment.id,
                                amount = activePayment.amount,
                                feeAmount = 0.0,
                                netAmount = activePayment.amount,
                                bank = activePayment.bank,
                                recipientName = activePayment.recipientName,
                                iban = activePayment.iban,
                                purpose = activePayment.purpose,
                                itemsSummary = activePayment.purpose,
                                timestamp = System.currentTimeMillis(),
                                status = TransactionStatus.PAID
                            )
                            val text = ReceiptGenerator.generateReceiptText(
                                transaction = dummyRecord,
                                cartItems = activePayment.cartSnapshot,
                                taxNumber = taxNumber,
                                merchant = merchant
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProReceiptLockedDialog) {
        AlertDialog(
            onDismissRequest = { showProReceiptLockedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text("Функція чеків у Pro", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Формування та надсилання справжніх електронних чеків текстового формату (через SMS, Email або месенджери) доступне у підписці QR POS Pro.")
                    Text("Вартість: 5$ на місяць або 55$ на рік (знижка -8%). Також ви отримуєте 0% комісії, повну аналітику та каталог товарів!", fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProReceiptLockedDialog = false
                        onDismiss()
                        onOpenSubscription()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.Black
                    )
                ) {
                    Text("Перейти до Pro", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showProReceiptLockedDialog = false }) {
                    Text("Зрозуміло")
                }
            }
        )
    }

    if (showBluetoothPrinterDialog) {
        val currentTx = TransactionRecord(
            id = activePayment.id,
            amount = activePayment.amount,
            feeAmount = 0.0,
            netAmount = activePayment.amount,
            bank = activePayment.bank,
            recipientName = activePayment.recipientName,
            iban = activePayment.iban,
            purpose = activePayment.purpose,
            itemsSummary = activePayment.purpose,
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.PAID
        )
        BluetoothPrinterDialog(
            transaction = currentTx,
            merchant = merchant,
            onDismiss = { showBluetoothPrinterDialog = false }
        )
    }
}

@Composable
private fun RequisiteRow(
    title: String,
    value: String,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
