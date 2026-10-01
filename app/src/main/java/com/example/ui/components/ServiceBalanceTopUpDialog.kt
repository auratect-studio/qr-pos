package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.config.PlatformBillingConfig
import com.example.model.MerchantProfile
import com.example.ui.theme.GoldPremium
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ServiceBalanceTopUpDialog(
    currentBalance: Double,
    merchant: MerchantProfile,
    isPremium: Boolean,
    onTopUpSuccess: (Double, String) -> Unit,
    onOpenSubscription: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var selectedAmount by remember { mutableDoubleStateOf(PlatformBillingConfig.TOP_UP_PRESETS.first()) }
    var customAmountText by remember { mutableStateOf("") }
    var isCustomAmount by remember { mutableStateOf(false) }
    var showIbanDetails by remember { mutableStateOf(false) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    var showCelebration by remember { mutableStateOf(false) }
    var celebrationAmount by remember { mutableDoubleStateOf(0.0) }

    // Анімований плавний лічильник балансу
    val animatedBalance by animateFloatAsState(
        targetValue = currentBalance.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animatedBalance"
    )

    // Автоматичне скидання підказки копіювання
    LaunchedEffect(copiedNotice) {
        if (copiedNotice != null) {
            delay(2000)
            copiedNotice = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Баланс обслуговування",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Комісія платформи (0.8% та 0.5%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card: Current Balance with Smooth Rolling Counter Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = if (currentBalance <= 0.0) listOf(Color(0xFF7F1D1D), Color(0xFF450A0A))
                                else if (currentBalance < PlatformBillingConfig.LOW_BALANCE_THRESHOLD) listOf(Color(0xFFB45309), Color(0xFF78350F))
                                else listOf(Color(0xFF065F46), Color(0xFF022C22))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Поточний залишок рахунку",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isPremium) "PRO БЕЗЛІМІТ" else if (currentBalance <= 0) "ВИЧЕРПАНО" else "АКТИВНИЙ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPremium) Color(0xFFFDE047) else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Animated Balance
                        Text(
                            text = String.format(Locale.US, "%.2f ₴", animatedBalance),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 38.sp
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val coveredTurnover = PlatformBillingConfig.calculateCoveredTurnover(currentBalance.coerceAtLeast(0.0))
                        Text(
                            text = if (isPremium) "👑 Безлімітні продажі без будь-яких комісій"
                            else "Залишок покриває ~${String.format(Locale.US, "%.0f", coveredTurnover)} ₴ торгового обороту",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Temporary copy feedback badge
                AnimatedVisibility(
                    visible = copiedNotice != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = copiedNotice ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section: Select Top-Up Amount
                Text(
                    text = "Оберіть суму поповнення:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformBillingConfig.TOP_UP_PRESETS.forEach { preset ->
                        val isSelected = !isCustomAmount && selectedAmount == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                isCustomAmount = false
                                selectedAmount = preset
                            },
                            label = {
                                Text(
                                    text = "+${preset.toInt()} ₴",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom amount field
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = {
                        customAmountText = it.filter { char -> char.isDigit() || char == '.' }
                        val parsed = customAmountText.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            selectedAmount = parsed
                            isCustomAmount = true
                        }
                    },
                    label = { Text("Інша сума (₴)") },
                    placeholder = { Text("Наприклад: 300") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val activeAmount = if (isCustomAmount) {
                    customAmountText.toDoubleOrNull() ?: selectedAmount
                } else selectedAmount

                val newTurnover = PlatformBillingConfig.calculateCoveredTurnover(activeAmount)
                Text(
                    text = "💡 +${String.format(Locale.US, "%.0f", activeAmount)} ₴ додасть ~${String.format(Locale.US, "%.0f", newTurnover)} ₴ до ліміту безперебійного прийому оплат",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp)
                )

                // Action Buttons for Payment
                // 1. Google Pay / Apple Pay / Bank Card
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PlatformBillingConfig.MASTER_PAYMENT_URL)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = Color.White)
                        Text(
                            text = "Оплатити через Google Pay / Картку",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Direct IBAN Requisites Accordion
                OutlinedButton(
                    onClick = { showIbanDetails = !showIbanDetails },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null)
                        Text(
                            text = if (showIbanDetails) "Сховати реквізити IBAN ▲" else "Оплатити за реквізитами IBAN (СЕП) ▼",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                AnimatedVisibility(visible = showIbanDetails) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Реквізити платформи для оплати:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )

                            // Recipient
                            Column {
                                Text("Отримувач:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(PlatformBillingConfig.MASTER_RECIPIENT_NAME, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }

                            // IBAN
                            Column {
                                Text("IBAN:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = PlatformBillingConfig.MASTER_IBAN,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(PlatformBillingConfig.MASTER_IBAN))
                                            copiedNotice = "IBAN платформи скопійовано!"
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Скопіювати IBAN", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Tax number
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("ЄДРПОУ / ІПН:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(PlatformBillingConfig.MASTER_TAX_NUMBER, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(PlatformBillingConfig.MASTER_TAX_NUMBER))
                                        copiedNotice = "Податковий номер скопійовано!"
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Скопіювати ЄДРПОУ", modifier = Modifier.size(16.dp))
                                }
                            }

                            // Payment purpose
                            val purpose = "${PlatformBillingConfig.PAYMENT_PURPOSE_PREFIX} ${merchant.merchantId}"
                            Column {
                                Text("Призначення платежу:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(purpose, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(purpose))
                                            copiedNotice = "Призначення скопійовано!"
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Скопіювати призначення", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Instant Demo / Test Top-Up Button (celebration test)
                Button(
                    onClick = {
                        celebrationAmount = activeAmount
                        showCelebration = true
                        onTopUpSuccess(activeAmount, "Тестове поповнення балансу")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("demo_top_up_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, tint = Color.White)
                        Text(
                            text = "⚡ Миттєво зарахувати +${String.format(Locale.US, "%.0f", activeAmount)} ₴ (Тест/Демо)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Pro Subscription Alternative Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onOpenSubscription()
                        },
                    colors = CardDefaults.cardColors(containerColor = GoldPremium.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldPremium, Color(0xFFF59E0B))))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPremium),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Не хочете слідкувати за балансом?",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Підключіть Безліміт PRO за 199 ₴/міс: 0% комісії та безперебійна робота!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
