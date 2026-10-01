package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import com.example.ui.components.BluetoothPrinterDialog
import com.example.util.ReceiptGenerator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransactionRecord
import com.example.ui.theme.GoldPremium
import com.example.viewmodel.PosUiState
import com.example.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onBack: () -> Unit,
    onOpenSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.merchant.isPremium) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Аналітика виручки",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFEF3C7))
                        .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Аналітика доступна у підписці Pro",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Детальна статистика надходжень: скільки зароблено за день, тиждень, місяць або обраний період від дати X до Y. Візуалізація графіками, розрахунок комісій та експорт офіційного звіту на пристрій.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Що надає Pro Аналітика:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AnalyticsBullet("Статистика за день, тиждень, місяць, сезон")
                        AnalyticsBullet("Інтерактивні графіки динаміки надходжень")
                        AnalyticsBullet("Звіти за індивідуальний період (від дати X до Y)")
                        AnalyticsBullet("Збереження PDF-звіту для бухгалтерії та ФОП")
                        AnalyticsBullet("0% комісія за всі транзакції замість 0.5%")
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenSubscription,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("unlock_analytics_pro_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Підключити Pro (5$/міс або 55$/рік • знижка 8%)",
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.togglePremium() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Активувати тестовий Pro доступ (0% ком.)")
                }
            }
        }
        return
    }

    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf("Сьогодні") }
    var selectedTxForDetails by remember { mutableStateOf<TransactionRecord?>(null) }
    var showBluetoothDialogForTx by remember { mutableStateOf<TransactionRecord?>(null) }
    val periods = listOf("Сьогодні", "7 днів", "30 днів", "Всього")

    val now = System.currentTimeMillis()
    val filteredTransactions = remember(selectedPeriod, uiState.transactions) {
        when (selectedPeriod) {
            "Сьогодні" -> uiState.transactions.filter { now - it.timestamp <= 1000L * 60 * 60 * 24 }
            "7 днів" -> uiState.transactions.filter { now - it.timestamp <= 1000L * 60 * 60 * 24 * 7 }
            "30 днів" -> uiState.transactions.filter { now - it.timestamp <= 1000L * 60 * 60 * 24 * 30 }
            else -> uiState.transactions
        }
    }

    val totalRevenue = filteredTransactions.sumOf { it.amount }
    val totalFee = filteredTransactions.sumOf { it.feeAmount }
    val totalNet = filteredTransactions.sumOf { it.netAmount }
    val transactionCount = filteredTransactions.size
    val averageCheck = if (transactionCount > 0) totalRevenue / transactionCount else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Аналітика виручки",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Фінансова статистика та звіти",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Export button
            IconButton(
                onClick = {
                    val reportText = buildFinancialReport(
                        period = selectedPeriod,
                        merchant = uiState.merchant.businessName,
                        totalRevenue = totalRevenue,
                        totalFee = totalFee,
                        totalNet = totalNet,
                        count = transactionCount,
                        transactions = filteredTransactions
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Фінансовий звіт QR POS ($selectedPeriod)")
                        putExtra(Intent.EXTRA_TEXT, reportText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Експорт фінансового звіту"))
                }
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Експорт звіту",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Period filter pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(periods) { period ->
                val isSelected = period == selectedPeriod
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedPeriod = period }
                ) {
                    Text(
                        text = period,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero metric card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Загальна виручка ($selectedPeriod)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.2f", totalRevenue)} ₴",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Транзакцій", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$transactionCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Середній чек", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.US, "%.2f", averageCheck)} ₴", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Комісія (0.5%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (uiState.merchant.isPremium) "0.00 ₴ (Pro)" else "-${String.format(Locale.US, "%.2f", totalFee)} ₴",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.merchant.isPremium) GoldPremium else MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Bar Chart Visualizer (Jetpack Compose Canvas)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Графік надходжень",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Bar Chart
                        RevenueBarChart(
                            transactions = filteredTransactions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                }
            }

            // Pro Subscription banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenSubscription() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.merchant.isPremium) GoldPremium.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldPremium),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Black)
                            }
                            Column {
                                Text(
                                    text = if (uiState.merchant.isPremium) "Преміум підписка активна (0% комісія)" else "Підключити Premium (0% комісія)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (uiState.merchant.isPremium) GoldPremium else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.merchant.isPremium) "Ви заощаджуєте 0.5% з кожного платежу" else "5$/місяць або 55$/рік • Економія коштів",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Transaction History Header
            item {
                Text(
                    text = "Історія операцій",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Transaction items
            items(filteredTransactions) { tx ->
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date(tx.timestamp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { selectedTxForDetails = tx }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.itemsSummary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${tx.bank.shortName} • $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${String.format(Locale.US, "%.2f", tx.amount)} ₴",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = if (tx.feeAmount > 0) "ком: -${String.format(Locale.US, "%.2f", tx.feeAmount)} ₴" else "без комісії",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    selectedTxForDetails?.let { tx ->
        val receiptText = ReceiptGenerator.generateReceiptText(
            transaction = tx,
            taxNumber = uiState.merchant.taxNumber,
            merchant = uiState.merchant
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedTxForDetails = null },
            title = {
                Text(text = "Чек №${tx.id.takeLast(6).uppercase(Locale.ROOT)}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "${tx.bank.displayName}\nСума: +${String.format(Locale.US, "%.2f", tx.amount)} ₴\nПризначення: ${tx.itemsSummary}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = selectedTxForDetails
                        selectedTxForDetails = null
                        showBluetoothDialogForTx = current
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Друк (Bluetooth)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Чек №${tx.id.takeLast(6)}")
                            putExtra(Intent.EXTRA_TEXT, receiptText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Поділитися чеком"))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Поділитися")
                }
            }
        )
    }

    showBluetoothDialogForTx?.let { tx ->
        BluetoothPrinterDialog(
            transaction = tx,
            merchant = uiState.merchant,
            onDismiss = { showBluetoothDialogForTx = null }
        )
    }
}

@Composable
private fun RevenueBarChart(
    transactions: List<TransactionRecord>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    // Group into 6 time slices or dummy bars if empty
    val bars = remember(transactions) {
        if (transactions.isEmpty()) {
            listOf(200f, 450f, 300f, 600f, 150f, 750f)
        } else {
            val amounts = transactions.map { it.amount.toFloat() }
            if (amounts.size < 6) amounts + List(6 - amounts.size) { 100f }
            else amounts.take(6)
        }
    }

    val maxVal = (bars.maxOrNull() ?: 1000f).coerceAtLeast(100f)

    Canvas(modifier = modifier) {
        val count = bars.size
        val gap = 16.dp.toPx()
        val totalGaps = gap * (count - 1)
        val barWidth = (size.width - totalGaps) / count

        bars.forEachIndexed { index, value ->
            val left = index * (barWidth + gap)
            val normalizedHeight = (value / maxVal) * (size.height * 0.85f)
            val top = size.height - normalizedHeight

            // Background track bar
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Active bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, normalizedHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

private fun buildFinancialReport(
    period: String,
    merchant: String,
    totalRevenue: Double,
    totalFee: Double,
    totalNet: Double,
    count: Int,
    transactions: List<TransactionRecord>
): String {
    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
    val sb = StringBuilder()
    sb.append("========================================\n")
    sb.append("      ФІНАНСОВИЙ ЗВІТ QR POS           \n")
    sb.append("========================================\n")
    sb.append("Продавець: $merchant\n")
    sb.append("Період: $period\n")
    sb.append("Сформовано: $dateStr\n")
    sb.append("----------------------------------------\n")
    sb.append("Загальний обіг (виручка): ${String.format(Locale.US, "%.2f", totalRevenue)} ₴\n")
    sb.append("Кількість операцій: $count\n")
    sb.append("Утримано комісій (0.5%): ${String.format(Locale.US, "%.2f", totalFee)} ₴\n")
    sb.append("Чистий дохід продавця: ${String.format(Locale.US, "%.2f", totalNet)} ₴\n")
    sb.append("----------------------------------------\n")
    sb.append("ДЕТАЛІЗАЦІЯ ОПЕРАЦІЙ:\n")
    transactions.forEachIndexed { index, tx ->
        val txDate = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
        sb.append("${index + 1}. $txDate | ${tx.bank.shortName} | ${tx.itemsSummary} | +${String.format(Locale.US, "%.2f", tx.amount)} ₴\n")
    }
    sb.append("========================================\n")
    sb.append("QR POS Terminal © 2026\n")
    return sb.toString()
}

@Composable
fun AnalyticsBullet(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
