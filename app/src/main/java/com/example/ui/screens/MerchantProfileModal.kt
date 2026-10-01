package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import com.example.config.PlatformBillingConfig
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import com.example.model.MerchantProfile
import com.example.model.UkrainianBank
import com.example.network.webhook.StreamConnectionState
import com.example.ui.theme.GoldPremium
import com.example.util.StaticQrPosterGenerator
import java.util.Locale

private val presetIcons = listOf(
    "🏪", "☕", "🛍️", "🍽️", "🥐", "✂️", "📱", "🚗", "🌿", "🏋️", "💼", "🏛️", "🍕", "🧁", "💎"
)

@Composable
fun MerchantProfileModal(
    currentProfile: MerchantProfile,
    onSaveProfile: (MerchantProfile) -> Unit,
    onDismiss: () -> Unit,
    onClearTokens: () -> Unit = {},
    onResetProfile: () -> Unit = {},
    streamConnectionState: StreamConnectionState = StreamConnectionState.DISCONNECTED,
    isRegisteringWebhook: Boolean = false,
    onRegisterMonobankWebhook: ((android.content.Context) -> Unit)? = null,
    onTestWebhookPayment: (() -> Unit)? = null,
    onRetryGatewayConnection: (() -> Unit)? = null,
    isAutoFillingFromMono: Boolean = false,
    onAutoFillFromMonobank: ((String) -> Unit)? = null,
    onShowSetPinDialog: () -> Unit = {},
    onToggleBiometric: (Boolean) -> Unit = {},
    onToggleVoiceCashier: (Boolean) -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onOpenTopUp: () -> Unit = {},
    pendingOfflineReceiptsCount: Int = 0,
    onSyncOfflineReceipts: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }

    var businessName by remember { mutableStateOf(currentProfile.businessName) }
    var taxNumber by remember { mutableStateOf(currentProfile.taxNumber) }
    var legalAddress by remember { mutableStateOf(currentProfile.legalAddress) }
    var iban by remember { mutableStateOf(currentProfile.iban) }
    var cardNumber by remember { mutableStateOf(currentProfile.cardNumber) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var email by remember { mutableStateOf(currentProfile.email) }
    var brandIcon by remember { mutableStateOf(currentProfile.brandIcon) }
    var tagline by remember { mutableStateOf(currentProfile.tagline) }
    var selectedBank by remember { mutableStateOf(currentProfile.defaultBank) }
    var monobankToken by remember { mutableStateOf(currentProfile.monobankToken) }
    var checkboxApiKey by remember { mutableStateOf(currentProfile.checkboxApiKey) }
    var autoFiscalizeWithCheckbox by remember { mutableStateOf(currentProfile.autoFiscalizeWithCheckbox) }
    var webhookGatewayUrl by remember { mutableStateOf(currentProfile.webhookGatewayUrl) }
    var customMerchantId by remember { mutableStateOf(currentProfile.customMerchantId) }
    var isMonoTokenVisible by remember { mutableStateOf(false) }
    var isCheckboxTokenVisible by remember { mutableStateOf(false) }
    var showDevSettings by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не вдалося відкрити посилання: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun pasteFromClipboard(): String? {
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.text?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                return text
            }
        }
        Toast.makeText(context, "Буфер обміну порожній", Toast.LENGTH_SHORT).show()
        return null
    }

    LaunchedEffect(currentProfile) {
        businessName = currentProfile.businessName
        taxNumber = currentProfile.taxNumber
        legalAddress = currentProfile.legalAddress
        iban = currentProfile.iban
        cardNumber = currentProfile.cardNumber
        phone = currentProfile.phone
        email = currentProfile.email
        brandIcon = currentProfile.brandIcon
        tagline = currentProfile.tagline
        selectedBank = currentProfile.defaultBank
        monobankToken = currentProfile.monobankToken
        checkboxApiKey = currentProfile.checkboxApiKey
        webhookGatewayUrl = currentProfile.webhookGatewayUrl
        customMerchantId = currentProfile.customMerchantId
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Clean Header Bar (No squished buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрити",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column {
                            Text(
                                text = "Акаунт та реквізити",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp
                                )
                            )
                            Text(
                                text = "Налаштування бізнесу, API та чеків",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Helpful Guidance Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "💡", fontSize = 14.sp)
                        Text(
                            text = "Для збереження налаштувань прогортайте сторінку в самий низ і натисніть кнопку «Зберегти реквізити».",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 💰 Картка Балансу обслуговування платформи
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentProfile.isPremium) GoldPremium.copy(alpha = 0.12f)
                            else if (currentProfile.serviceBalance <= 0) Color(0xFFFEF2F2)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = if (currentProfile.isPremium) Brush.horizontalGradient(listOf(GoldPremium, Color(0xFFF59E0B)))
                            else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (currentProfile.isPremium) GoldPremium else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Баланс обслуговування",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (currentProfile.isPremium) GoldPremium
                                            else if (currentProfile.serviceBalance <= 0) Color(0xFFEF4444)
                                            else Color(0xFF10B981)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (currentProfile.isPremium) "PRO 0%" else if (currentProfile.serviceBalance <= 0) "Вичерпано" else "Активний (0.8%)",
                                        color = if (currentProfile.isPremium) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "Залишок коштів:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (currentProfile.isPremium) "Безліміт" else String.format(Locale.US, "%.2f ₴", currentProfile.serviceBalance),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (currentProfile.isPremium) GoldPremium
                                        else if (currentProfile.serviceBalance <= 0) Color(0xFFDC2626)
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (!currentProfile.isPremium) {
                                    val covered = PlatformBillingConfig.calculateCoveredTurnover(currentProfile.serviceBalance.coerceAtLeast(0.0))
                                    Text(
                                        text = "Покриває ~${String.format(Locale.US, "%.0f", covered)} ₴ продажів",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onOpenTopUp() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Поповнити", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onOpenSubscription() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (currentProfile.isPremium) "Керувати PRO" else "Тарифи PRO", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Quick Demo Presets
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "⚡ Швидкі шаблони бізнесу (клікніть для автозаповнення):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PresetChip("☕ Кав'ярня 'Aroma'") {
                                    brandIcon = "☕"
                                    businessName = "Кав'ярня 'Aroma Coffee'"
                                    taxNumber = "3198765432"
                                    legalAddress = "м. Київ, вул. Велика Васильківська, 14"
                                    selectedBank = UkrainianBank.MONOBANK
                                    iban = "UA123220010000026001234567890"
                                    cardNumber = "4441 1144 2233 4455"
                                    phone = "+380 67 111 22 33"
                                    email = "aroma.kyiv@gmail.com"
                                    tagline = "Смачна кава та свіжі десерти щодня! ☕✨"
                                }
                                PresetChip("🛍️ Бутік 'Trend UA'") {
                                    brandIcon = "🛍️"
                                    businessName = "Бутік одягу 'Trend UA'"
                                    taxNumber = "2899123456"
                                    legalAddress = "м. Львів, пл. Ринок, 8"
                                    selectedBank = UkrainianBank.PRIVATBANK
                                    iban = "UA233052990000026009876543210"
                                    cardNumber = "5168 7573 8899 0011"
                                    phone = "+380 50 333 44 55"
                                    email = "trend.lviv@gmail.com"
                                    tagline = "Одяг українських дизайнерів з любов'ю 🇺🇦"
                                }
                                PresetChip("👨‍💼 ФОП Шевченко (IT/Консалтинг)") {
                                    brandIcon = "💼"
                                    businessName = "ФОП Шевченко О. В."
                                    taxNumber = "3123456789"
                                    legalAddress = "м. Київ, вул. Хрещатик, 22"
                                    selectedBank = UkrainianBank.MONOBANK
                                    iban = "UA683220010000026001234567890"
                                    cardNumber = "4441 1144 5566 7788"
                                    phone = "+380 97 123 45 67"
                                    email = "shevchenko.consult@gmail.com"
                                    tagline = "Дякуємо за співпрацю! Слава Україні! 🇺🇦"
                                }
                                PresetChip("✂️ Салон краси 'Beauty Lab'") {
                                    brandIcon = "✂️"
                                    businessName = "Салон краси 'Beauty Lab'"
                                    taxNumber = "3456789012"
                                    legalAddress = "м. Одеса, вул. Дерибасівська, 19"
                                    selectedBank = UkrainianBank.PUMB
                                    iban = "UA343348510000026003456789012"
                                    cardNumber = "4149 4990 1234 5678"
                                    phone = "+380 63 999 88 77"
                                    email = "beauty.odesa@gmail.com"
                                    tagline = "Ваша краса та сяйво — наша турбота! 💄"
                                }
                            }
                        }
                    }

                    // Section 1: Brand & Logo
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "1. Логотип та бренд магазину",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Цей логотип відображається у верхній шапці термінала та друкується у чеку для клієнта:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Preset icon chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetIcons.forEach { iconEmoji ->
                                    val isSelected = brandIcon == iconEmoji
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { brandIcon = iconEmoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = iconEmoji, fontSize = 22.sp)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = brandIcon,
                                    onValueChange = { brandIcon = it.take(4) },
                                    label = { Text("Власний символ / Емодзі") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )

                                // Live Preview Badge
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = brandIcon.ifBlank { "⚡" },
                                        fontSize = 28.sp
                                    )
                                }
                            }
                        }
                    }

                    // Section 2: Business & Legal Details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "2. Дані організації / Магазину / ПІБ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                label = { Text("Назва організації, компанії, магазину або ПІБ") },
                                placeholder = { Text("Напр. Кав'ярня 'Coffee Bean' або ФОП Шевченко О. В.") },
                                leadingIcon = {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_business_name"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            OutlinedTextField(
                                value = taxNumber,
                                onValueChange = { taxNumber = it.filter { ch -> ch.isDigit() }.take(10) },
                                label = { Text("Код ЄДРПОУ або ІПН (8-10 цифр)") },
                                placeholder = { Text("3123456789") },
                                leadingIcon = {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_tax_number"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            OutlinedTextField(
                                value = legalAddress,
                                onValueChange = { legalAddress = it },
                                label = { Text("Юридична адреса або місце торгівлі") },
                                placeholder = { Text("м. Київ, вул. Хрещатик, 22") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_address"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    // Section 3: Banking & Payment Requisites
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "3. Банківські реквізити для зарахування коштів",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Оберіть основний обслуговуючий банк:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Bank Selector
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UkrainianBank.entries.forEach { bank ->
                                    val isSelected = selectedBank == bank
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) Color(bank.brandColorHex)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedBank = bank
                                                if (iban.isBlank() || iban.startsWith("UA")) {
                                                    iban = bank.defaultIban
                                                }
                                                if (cardNumber.isBlank()) {
                                                    cardNumber = bank.defaultCardNumber
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = bank.shortName,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = iban,
                                onValueChange = { input ->
                                    val clean = input.uppercase().filter { it.isLetterOrDigit() }.take(29)
                                    iban = clean
                                },
                                label = { Text("Діючий номер рахунку IBAN (29 знаків)") },
                                placeholder = { Text("UA233052990000026001234567890") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_iban"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(16)
                                    cardNumber = digits.chunked(4).joinToString(" ")
                                },
                                label = { Text("Номер платіжної картки (16 цифр для P2P)") },
                                placeholder = { Text("4441 1144 5566 7788") },
                                leadingIcon = {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_card_number"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    // Section 4: Contact & Receipt Extras
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "4. Контакти та примітки для чека",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Контактний номер телефону") },
                                placeholder = { Text("+380 67 123 45 67") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email для звітів та клієнтів") },
                                placeholder = { Text("shop@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            OutlinedTextField(
                                value = tagline,
                                onValueChange = { tagline = it },
                                label = { Text("Побажання / Слоган у чеку") },
                                placeholder = { Text("Дякуємо за покупку! Слава Україні! 🇺🇦") },
                                leadingIcon = {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    // Section 5: Integrations & Real Banking APIs
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "5. Реальні інтеграції: Monobank API & ПРРО",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Підключіть персональний токен Monobank для автоматичного відстеження надходжень або ключ Checkbox для фіскалізації в ДПС:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Monobank Header Row with Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Monobank X-Token",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { openUrl("https://api.monobank.ua/") },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Отримати", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            pasteFromClipboard()?.let { monobankToken = it }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Вставити", fontSize = 11.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = monobankToken,
                                onValueChange = { monobankToken = it.trim() },
                                label = { Text("Monobank X-Token (api.monobank.ua)") },
                                placeholder = { Text("u2_xxxxxxxxxxxxxxxxxxxxxx") },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isMonoTokenVisible = !isMonoTokenVisible },
                                        modifier = Modifier.testTag("toggle_mono_token_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isMonoTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isMonoTokenVisible) "Сховати токен" else "Показати токен",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (isMonoTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_mono_token"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            // 1-Click Monobank Auto-Fill Button
                            Button(
                                onClick = {
                                    if (monobankToken.isBlank()) {
                                        Toast.makeText(context, "Спочатку введіть або вставте токен Monobank", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onAutoFillFromMonobank?.invoke(monobankToken)
                                    }
                                },
                                enabled = !isAutoFillingFromMono && monobankToken.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_autofill_from_mono"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isAutoFillingFromMono) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Отримання реквізитів з Monobank...")
                                } else {
                                    Text("🪄 Автозаповнення реквізитів з Monobank", fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "✨ Автоматично підтягне офіційну назву ФОП, IBAN та номер картки, щоб не вводити їх вручну!",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Checkbox Header Row with Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ключ ПРРО Checkbox",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { openUrl("https://my.checkbox.ua/") },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Кабінет", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            pasteFromClipboard()?.let { checkboxApiKey = it }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Вставити", fontSize = 11.sp)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = checkboxApiKey,
                                onValueChange = { checkboxApiKey = it.trim() },
                                label = { Text("Ключ касира Checkbox (Bearer токен)") },
                                placeholder = { Text("••••••••••••••••") },
                                leadingIcon = {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { isCheckboxTokenVisible = !isCheckboxTokenVisible },
                                        modifier = Modifier.testTag("toggle_checkbox_token_visibility")
                                    ) {
                                        Icon(
                                            imageVector = if (isCheckboxTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isCheckboxTokenVisible) "Сховати токен" else "Показати токен",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                visualTransformation = if (isCheckboxTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_checkbox_key"),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Text(
                                text = "💡 Як отримати: зареєструйте касу на https://my.checkbox.ua/ та скопіюйте API Pin/Bearer токен касира.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Switch: Auto-fiscalization
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "⚡ Автоматична фіскалізація (ПРРО)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Автоматично реєструвати кожен оплачений чек у Checkbox та ДПС",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoFiscalizeWithCheckbox,
                                    onCheckedChange = { autoFiscalizeWithCheckbox = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF10B981),
                                        checkedTrackColor = Color(0xFF064E3B)
                                    )
                                )
                            }

                            // Offline receipts queue status
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (pendingOfflineReceiptsCount > 0) Color(0xFF064E3B).copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.dp, if (pendingOfflineReceiptsCount > 0) Color(0xFF10B981) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "📴 Офлайн-буферизація чеків",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (pendingOfflineReceiptsCount > 0)
                                                "У черзі на фіскалізацію: $pendingOfflineReceiptsCount чеків"
                                            else
                                                "Черга синхронізована (0 чеків очікує). Працює під час блекаутів.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (pendingOfflineReceiptsCount > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onSyncOfflineReceipts() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Синхронізувати", fontSize = 11.sp)
                                    }
                                }
                            }

                            if (monobankToken.isNotBlank() || checkboxApiKey.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        monobankToken = ""
                                        checkboxApiKey = ""
                                        onClearTokens()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_clear_secure_tokens")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Очистити збережені токени (Keystore)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Section 6: Live Webhook Gateway & Миттєва синхронізація
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF10B981))
                                Text(
                                    text = "6. Live Webhook & Миттєва синхронізація",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Миттєве зарахування оплат через прямі банківські вебхуки та Server-Sent Events (SSE). Швидко, надійно і без зайвих налаштувань.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Status badge
                            val (streamDot, streamLabel, streamColor) = when (streamConnectionState) {
                                StreamConnectionState.CONNECTED -> Triple("🟢", "Live Gateway підключено (SSE активний)", Color(0xFF10B981))
                                StreamConnectionState.CONNECTING -> Triple("🟡", "Підключення до Live шлюзу...", Color(0xFFF59E0B))
                                else -> Triple("⚪", "Live Webhook офлайн (пряма перевірка активна)", MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = streamDot, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = streamLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = streamColor
                                    )
                                }
                                if (streamConnectionState != StreamConnectionState.CONNECTED && onRetryGatewayConnection != null) {
                                    TextButton(
                                        onClick = onRetryGatewayConnection,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("Повторити", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 1-Click Connect Webhook Button
                            if (onRegisterMonobankWebhook != null) {
                                Button(
                                    onClick = { onRegisterMonobankWebhook(context) },
                                    enabled = monobankToken.isNotBlank() && !isRegisteringWebhook,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_register_mono_webhook"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isRegisteringWebhook) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Реєстрація Webhook у банку...")
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("⚡ Увімкнути миттєве сповіщення про оплату", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "💡 Автоматично реєструє вебхук у Monobank для вашого акаунта. Без введення технічних параметрів.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Expandable Developer Settings Spoiler
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showDevSettings = !showDevSettings }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "⚙️ Налаштування власного сервера (для розробників)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = if (showDevSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = showDevSettings) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = webhookGatewayUrl,
                                        onValueChange = { webhookGatewayUrl = it },
                                        label = { Text("URL шлюзу (Edge Gateway / Cloudflare)") },
                                        placeholder = { Text("https://qr-pos-gateway.antigravity.workers.dev") },
                                        leadingIcon = {
                                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_profile_gateway_url"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp)
                                    )

                                    OutlinedTextField(
                                        value = customMerchantId,
                                        onValueChange = { customMerchantId = it },
                                        label = { Text("Ідентифікатор мерчанта (merchantId)") },
                                        placeholder = { Text(if (taxNumber.isNotBlank()) "fop-$taxNumber" else "fop-demo") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_profile_merchant_id"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp)
                                    )

                                    val effectiveMerchantId = customMerchantId.ifBlank {
                                        if (taxNumber.isNotBlank()) "fop-$taxNumber" else "merchant-demo"
                                    }
                                    val calculatedWebhookUrl = "${webhookGatewayUrl.trimEnd('/')}/webhook/mono/$effectiveMerchantId"

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Згенерований Webhook URL для банку:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = calculatedWebhookUrl,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                ),
                                                color = Color(0xFF67E8F9)
                                            )
                                        }
                                    }

                                    if (onTestWebhookPayment != null) {
                                        OutlinedButton(
                                            onClick = { onTestWebhookPayment() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("btn_test_webhook_push"),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Надіслати тестовий імпульс через Gateway", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Receipt Preview Snippet
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "📄 Зразок заголовка вашого електронного чека:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = """
                                        ================================
                                               ${brandIcon.ifBlank { "⚡" }}  ЕЛЕКТРОННИЙ ЧЕК  ${brandIcon.ifBlank { "⚡" }}
                                                QR POS TERMINAL         
                                        ================================
                                        Організація: ${businessName.ifBlank { "Мій Бізнес" }}
                                        ІПН/ЄДРПОУ: ${taxNumber.ifBlank { "3123456789" }}
                                        Адреса: ${legalAddress.ifBlank { "м. Київ" }}
                                        Тел: ${phone.ifBlank { "+380 ..." }}
                                        Банк: ${selectedBank.shortName}
                                        Рахунок: ${iban.ifBlank { selectedBank.defaultIban }}
                                        Картка: ${if (cardNumber.isNotBlank()) cardNumber.take(4) + " •••• •••• " + cardNumber.takeLast(4) else "—"}
                                        --------------------------------
                                        ${tagline.ifBlank { "Дякуємо за покупку!" }}
                                    """.trimIndent(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 1. Security Card (PIN & Biometric Lock - Free for all)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (currentProfile.isPinProtected) "🔒" else "🔓",
                                    fontSize = 18.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Безпека та захист налаштувань",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentProfile.isPinProtected)
                                            "Захищено 4-значним PIN-кодом"
                                        else
                                            "Встановіть PIN для захисту IBAN та токенів",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (currentProfile.isPinProtected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { onShowSetPinDialog() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (currentProfile.isPinProtected)
                                        "Змінити або вимкнути PIN-код"
                                    else
                                        "Встановити 4-значний PIN-код",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (currentProfile.isPinProtected) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Біометрична авторизація",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Вхід за відбитком пальця або Face ID",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = currentProfile.biometricEnabled,
                                        onCheckedChange = { onToggleBiometric(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF10B981),
                                            checkedTrackColor = Color(0xFF064E3B)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 2. PRO Features Card (Voice Cashier & Static QR Poster)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "👑", fontSize = 18.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Преміум функції (PRO)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentProfile.isPremium) "Всі PRO можливості активні" else "0% комісія та розширені інструменти",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (currentProfile.isPremium) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                }
                            }

                            // Voice Cashier
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "🗣️ Голосовий касир",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!currentProfile.isPremium) {
                                            Text(
                                                text = "PRO",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFFF59E0B)
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF78350F).copy(alpha = 0.4f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Озвучує зараховану суму українською після оплати",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (currentProfile.isPremium) {
                                    Switch(
                                        checked = currentProfile.voiceCashierEnabled,
                                        onCheckedChange = { onToggleVoiceCashier(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF10B981),
                                            checkedTrackColor = Color(0xFF064E3B)
                                        )
                                    )
                                } else {
                                    TextButton(onClick = { onOpenSubscription() }) {
                                        Text("Активувати", color = Color(0xFFF59E0B), fontSize = 12.sp)
                                    }
                                }
                            }

                            // Static QR Poster Generator
                            OutlinedButton(
                                onClick = {
                                    if (currentProfile.isPremium) {
                                        try {
                                            val pdfFile = StaticQrPosterGenerator.generatePosterPdf(context, currentProfile)
                                            StaticQrPosterGenerator.shareOrPrintPoster(context, pdfFile)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Помилка створення постера: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        onOpenSubscription()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (currentProfile.isPremium) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = if (currentProfile.isPremium)
                                        "🖨️ Зберегти/Роздрукувати постер A4 з QR"
                                    else
                                        "🖨️ Друкований постер A4 з QR (PRO 👑)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Bottom Save Button
                    Button(
                        onClick = {
                            val updated = currentProfile.copy(
                                businessName = businessName.trim().ifBlank { "Мій Бізнес" },
                                taxNumber = taxNumber.trim(),
                                legalAddress = legalAddress.trim(),
                                defaultBank = selectedBank,
                                iban = iban.trim().ifBlank { selectedBank.defaultIban },
                                cardNumber = cardNumber.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                brandIcon = brandIcon.trim().ifBlank { "⚡" },
                                tagline = tagline.trim(),
                                monobankToken = monobankToken.trim(),
                                checkboxApiKey = checkboxApiKey.trim(),
                                autoFiscalizeWithCheckbox = autoFiscalizeWithCheckbox,
                                webhookGatewayUrl = webhookGatewayUrl.trim().ifBlank { "https://qr-pos-gateway.antigravity.workers.dev" },
                                customMerchantId = customMerchantId.trim()
                            )
                            onSaveProfile(updated)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("save_merchant_profile_bottom_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Зберегти реквізити організації",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            businessName = ""
                            taxNumber = ""
                            legalAddress = ""
                            cardNumber = ""
                            phone = ""
                            email = ""
                            monobankToken = ""
                            checkboxApiKey = ""
                            autoFiscalizeWithCheckbox = false
                            onResetProfile()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("reset_merchant_profile_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Скинути налаштування",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
