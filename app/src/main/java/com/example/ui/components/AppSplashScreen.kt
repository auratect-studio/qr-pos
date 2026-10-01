package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Стильний анімований вступний сплеш-екран при вході в додаток.
 * Плавна поява 3D-іконки, пульсуюче неонове сяйво та фірмовий стиль Дія/One UI.
 */
@Composable
fun AppSplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.55f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }
    val pulseScale = remember { Animatable(0.85f) }
    val splashAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Пульсуюче м'яке неонове світіння позаду логотипу
        launch {
            pulseScale.animateTo(
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // Поява та пружинне масштабування 3D-іконки
        launch {
            scale.animateTo(
                targetValue = 1.05f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        }

        // Прозорість іконки
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(350, easing = LinearEasing)
            )
        }

        // Плавна поява назви та підзаголовка
        launch {
            delay(220)
            launch {
                textOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }

        // Загальний таймінг перегляду сплешу (~1.3с), потім плавне згасання
        delay(1250)
        splashAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        )
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(splashAlpha.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060B12),
                        Color(0xFF0B1424),
                        Color(0xFF040A10)
                    )
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Дозволяє миттєво пропустити сплеш кліком
                onSplashFinished()
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Контейнер іконки з неоновим гало-підсвічуванням
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Розмите смарагдове сяйво позаду
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = 0.35f),
                                    Color(0xFF059669).copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 3D-іконка додатку (растровий PNG високої роздільної здатності)
                Image(
                    painter = painterResource(id = R.drawable.app_logo_splash),
                    contentDescription = "QR POS Logo",
                    modifier = Modifier
                        .size(108.dp)
                        .scale(scale.value)
                        .alpha(alpha.value)
                        .clip(RoundedCornerShape(26.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Текстовий блок
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffsetY.value.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "QR POS",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color.White
                    )

                    // Неонова точка статусу
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Миттєві банківські платежі",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.5.sp
                    ),
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Нижній бейдж
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(textAlpha.value)
        ) {
            Text(
                text = "🇺🇦 Зроблено в Україні",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.8.sp
                ),
                color = Color(0xFF64748B)
            )
        }
    }
}
