package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AmountDisplay
import com.example.ui.components.BankSelector
import com.example.ui.components.FinancialKeypad
import com.example.viewmodel.PosUiState
import com.example.viewmodel.PosViewModel

@Composable
fun TerminalScreen(
    uiState: PosUiState,
    viewModel: PosViewModel,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Bank selector
            BankSelector(
                selectedBank = uiState.selectedBank,
                onBankSelected = { viewModel.selectBank(it) }
            )

            // 2. Amount & Commission display
            AmountDisplay(
                amount = uiState.effectiveAmount,
                isCartActive = uiState.cartItems.isNotEmpty(),
                cartItemCount = uiState.cartItems.sumOf { it.quantity },
                isPremium = uiState.merchant.isPremium,
                feeAmount = uiState.calculatedFee,
                netAmount = uiState.netToMerchant,
                onClearClick = {
                    if (uiState.cartItems.isNotEmpty()) viewModel.clearCart()
                    else viewModel.onKeypadInput("CLEAR")
                },
                onOpenCartClick = onOpenCatalog
            )

            // 3. Financial keypad & quick sums (or disabled when items are in cart)
            FinancialKeypad(
                onKeyClick = { viewModel.onKeypadInput(it) },
                onQuickAmount = { viewModel.setQuickAmount(it) },
                isCartActive = uiState.cartItems.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Products / Catalog selector button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenCatalog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_catalog_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.cartItems.isNotEmpty()) "Змінити кошик товарів (${uiState.cartItems.sumOf { it.quantity }})"
                        else "Вибрати товари або послуги",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!uiState.merchant.isPremium) {
                        Spacer(modifier = Modifier.width(6.dp))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Generate QR Button CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (!uiState.merchant.isPremium) {
                val balance = uiState.merchant.serviceBalance
                val isExhausted = balance <= 0.0
                val isLow = balance < 15.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isExhausted) Color(0xFFFEE2E2)
                            else if (isLow) Color(0xFFFEF3C7)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { viewModel.showTopUpModal(true) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExhausted) "⚠️ Баланс вичерпано (${String.format(java.util.Locale.US, "%.2f", balance)} ₴)"
                        else "💳 Баланс: ${String.format(java.util.Locale.US, "%.2f", balance)} ₴ (0.8%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isExhausted) Color(0xFFB91C1C)
                        else if (isLow) Color(0xFF92400E)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Поповнити ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = { viewModel.generatePayment() },
                enabled = uiState.effectiveAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_qr_btn"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Сформувати QR-код",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )
            }
        }
    }
}
