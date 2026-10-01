package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.PaymentSoundHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PaymentSuccessOverlay(
    isVisible: Boolean,
    amount: Double,
    tipsAmount: Double = 0.0,
    feeAmount: Double = 0.0,
    serviceBalance: Double = 100.0,
    isPremium: Boolean = false,
    businessName: String = "",
    onDismiss: () -> Unit
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            val context = LocalContext.current
            val offsetY = remember { Animatable(220f) }
            val scale = remember { Animatable(0.4f) }
            val checkScale = remember { Animatable(0.2f) }

            LaunchedEffect(Unit) {
                // Відтворюємо касовий дзвін "Дзинь-дзинь!" та тактильний відгук
                PaymentSoundHelper.playPaymentSuccessSound(context)

                offsetY.snapTo(220f)
                scale.snapTo(0.4f)
                checkScale.snapTo(0.2f)

                // Animate checkmark floating up from bottom to center
                launch {
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                launch {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    delay(150)
                    checkScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }

                // Auto dismiss after 2.8 seconds
                delay(2800)
                onDismiss()
            }

            Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF047857).copy(alpha = 0.96f),
                            Color(0xFF065F46).copy(alpha = 0.98f),
                            Color(0xFF022C22)
                        )
                    )
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onDismiss()
                }
                .testTag("payment_success_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.value.toInt()) }
                    .scale(scale.value)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Big Glowing Green Checkmark Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Успіх",
                        tint = Color(0xFF059669),
                        modifier = Modifier
                            .size(70.dp)
                            .scale(checkScale.value)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "ОПЛАЧЕНО!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format(Locale.US, "%.2f ₴", amount),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 42.sp
                    ),
                    color = Color(0xFFFDE047) // Golden accent
                )

                if (tipsAmount > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "(в т.ч. чайові: +${String.format(Locale.US, "%.2f", tipsAmount)} ₴)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                if (!isPremium && feeAmount > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Комісія: –${String.format(Locale.US, "%.2f", feeAmount)} ₴ • Залишок балансу: ${String.format(Locale.US, "%.2f", serviceBalance)} ₴",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFFDE68A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (businessName.isNotBlank()) businessName else "Дякуємо за покупку!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Торкніться в будь-якому місці для закриття",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}
}
