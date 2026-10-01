package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

enum class TipOption(val percentage: Int?, val label: String) {
    NONE(0, "Без чайових"),
    FIVE(5, "+5%"),
    TEN(10, "+10%"),
    FIFTEEN(15, "+15%"),
    CUSTOM(null, "Власна")
}

@Composable
fun TipsSelectorRow(
    baseAmount: Double,
    selectedTipAmount: Double,
    isPremium: Boolean,
    onTipSelected: (tipAmount: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    val tip5 = Math.round(baseAmount * 0.05 * 100.0) / 100.0
    val tip10 = Math.round(baseAmount * 0.10 * 100.0) / 100.0
    val tip15 = Math.round(baseAmount * 0.15 * 100.0) / 100.0

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Чайові персоналу",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Anti-abuse commission badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isPremium) Color(0xFF059669).copy(alpha = 0.2f)
                        else Color(0xFFF59E0B).copy(alpha = 0.2f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isPremium) "0% комісії Pro" else "Пільгова комісія 0.25%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isPremium) Color(0xFF34D399) else Color(0xFFFBBF24)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val options = listOf(
                Pair(0.0, "0 ₴"),
                Pair(tip5, if (tip5 > 0) "+5%" else "+5%"),
                Pair(tip10, if (tip10 > 0) "+10%" else "+10%"),
                Pair(tip15, if (tip15 > 0) "+15%" else "+15%"),
                Pair(-1.0, "Власна")
            )

            for ((amountVal, title) in options) {
                val isSelected = when {
                    amountVal == -1.0 -> selectedTipAmount > 0.0 && selectedTipAmount != tip5 && selectedTipAmount != tip10 && selectedTipAmount != tip15
                    amountVal == 0.0 -> selectedTipAmount == 0.0
                    else -> Math.abs(selectedTipAmount - amountVal) < 0.01
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (amountVal == -1.0) {
                                showCustomDialog = true
                            } else {
                                onTipSelected(amountVal)
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (selectedTipAmount > 0.0) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Чайові: +${String.format(Locale.US, "%.2f", selectedTipAmount)} ₴",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Разом: ${String.format(Locale.US, "%.2f", baseAmount + selectedTipAmount)} ₴",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Власна сума чайових") },
            text = {
                Column {
                    Text(
                        text = "Введіть суму чайових у гривнях (комісія ${if (isPremium) "0%" else "0.25%"}):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("Сума чайових (₴)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_tip_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customInput.toDoubleOrNull() ?: 0.0
                        if (parsed >= 0.0) {
                            onTipSelected(parsed)
                        }
                        showCustomDialog = false
                    }
                ) {
                    Text("Застосувати")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}
