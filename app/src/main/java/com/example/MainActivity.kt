package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.model.TransactionStatus
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.DiiaTopBar
import com.example.ui.components.PaymentSuccessOverlay
import com.example.ui.components.PinUnlockDialog
import com.example.ui.components.ServiceBalanceTopUpDialog
import com.example.ui.components.SetPinDialog
import com.example.ui.components.AppSplashScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CustomerFacingScreen
import com.example.ui.screens.MerchantProfileModal
import com.example.ui.screens.QrPaymentDialog
import com.example.ui.screens.SubscriptionModal
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PosViewModel

enum class AppTab {
    TERMINAL,
    CATALOG,
    ANALYTICS
}

class MainActivity : FragmentActivity() {

    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Exception) {}
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            } catch (_: Exception) {}
        }

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var currentTab by remember { mutableStateOf(AppTab.TERMINAL) }
    var isSplashVisible by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            DiiaTopBar(
                merchantName = uiState.merchant.businessName,
                isPremium = uiState.merchant.isPremium,
                onSubscriptionClick = { viewModel.showSubscriptionModal(true) },
                bannerNotification = uiState.inAppBannerNotification,
                onDismissBanner = { viewModel.clearBanner() },
                onProfileClick = { viewModel.requestProtectedAction { viewModel.showProfileModal(true) } },
                onTopUpClick = { viewModel.showTopUpModal(true) },
                serviceBalance = uiState.merchant.serviceBalance,
                lastBalanceDelta = uiState.lastBalanceDelta,
                brandIcon = uiState.merchant.brandIcon
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.TERMINAL,
                    onClick = { currentTab = AppTab.TERMINAL },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Термінал") },
                    label = {
                        Text(
                            text = "Термінал",
                            fontWeight = if (currentTab == AppTab.TERMINAL) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.CATALOG,
                    onClick = { currentTab = AppTab.CATALOG },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Товари") },
                    label = {
                        Text(
                            text = if (!uiState.merchant.isPremium) "Товари 🔒" else "Товари (${uiState.cartItems.sumOf { it.quantity }})",
                            fontWeight = if (currentTab == AppTab.CATALOG) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.ANALYTICS,
                    onClick = { currentTab = AppTab.ANALYTICS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Аналітика") },
                    label = {
                        Text(
                            text = if (!uiState.merchant.isPremium) "Аналітика 🔒" else "Аналітика",
                            fontWeight = if (currentTab == AppTab.ANALYTICS) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                AppTab.TERMINAL -> {
                    TerminalScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenCatalog = { currentTab = AppTab.CATALOG }
                    )
                }

                AppTab.CATALOG -> {
                    CatalogScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBackToTerminal = { currentTab = AppTab.TERMINAL },
                        onOpenSubscription = { viewModel.showSubscriptionModal(true) }
                    )
                }

                AppTab.ANALYTICS -> {
                    AnalyticsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { currentTab = AppTab.TERMINAL },
                        onOpenSubscription = { viewModel.showSubscriptionModal(true) }
                    )
                }
            }

            // QR Payment Dialog Overlay
            uiState.activePayment?.let { payment ->
                QrPaymentDialog(
                    activePayment = payment,
                    isPremium = uiState.merchant.isPremium,
                    taxNumber = uiState.merchant.taxNumber,
                    onDismiss = { viewModel.dismissPayment() },
                    onSimulatePayment = { ctx -> viewModel.simulateCustomerPayment(ctx) },
                    onOpenSubscription = { viewModel.showSubscriptionModal(true) },
                    merchant = uiState.merchant,
                    onCheckMonoPayment = { ctx -> viewModel.checkMonobankLivePayment(ctx) },
                    isCheckingMono = uiState.isCheckingMonoPayment,
                    monoCooldownSeconds = uiState.monoCooldownSeconds,
                    onSelectMode = { mode -> viewModel.setQrPaymentMode(mode) },
                    onFiscalizeCheckbox = { rec -> viewModel.fiscalizeWithCheckbox(rec) },
                    isFiscalizingCheckbox = uiState.isFiscalizingCheckbox,
                    lastFiscalCode = uiState.lastFiscalCheckCode,
                    streamConnectionState = uiState.streamConnectionState,
                    onTriggerTestWebhook = { viewModel.triggerTestWebhookPayment() },
                    onSelectTip = { tip -> viewModel.setTipAmount(tip) },
                    onOpenCustomerDisplay = { viewModel.openCustomerDisplay(true) }
                )
            }

            // Subscription Modal Dialog
            if (uiState.showSubscriptionModal) {
                SubscriptionModal(
                    isCurrentlyPremium = uiState.merchant.isPremium,
                    onTogglePremium = { viewModel.togglePremium() },
                    onDismiss = { viewModel.showSubscriptionModal(false) }
                )
            }

            // Merchant Account & Profile Modal Dialog
            if (uiState.showProfileModal) {
                MerchantProfileModal(
                    currentProfile = uiState.merchant,
                    onSaveProfile = { updated -> viewModel.updateMerchantProfile(updated) },
                    onDismiss = { viewModel.showProfileModal(false) },
                    onClearTokens = { viewModel.clearSecureTokens() },
                    onResetProfile = { viewModel.resetMerchantProfile() },
                    streamConnectionState = uiState.streamConnectionState,
                    isRegisteringWebhook = uiState.isRegisteringWebhook,
                    onRegisterMonobankWebhook = { ctx -> viewModel.registerMonobankWebhook(ctx) },
                    onTestWebhookPayment = { viewModel.triggerTestWebhookPayment() },
                    onRetryGatewayConnection = { viewModel.retryGatewayConnection() },
                    isAutoFillingFromMono = uiState.isAutoFillingFromMono,
                    onAutoFillFromMonobank = { token -> viewModel.autoFillFromMonobank(token) },
                    onShowSetPinDialog = { viewModel.showSetPinDialog(true) },
                    onToggleBiometric = { enabled -> viewModel.toggleBiometric(enabled) },
                    onToggleVoiceCashier = { enabled -> viewModel.toggleVoiceCashier(enabled) },
                    onOpenSubscription = { viewModel.showSubscriptionModal(true) },
                    onOpenTopUp = { viewModel.showTopUpModal(true) },
                    pendingOfflineReceiptsCount = uiState.pendingOfflineReceiptsCount,
                    onSyncOfflineReceipts = { viewModel.syncOfflineReceiptsNow() }
                )
            }

            // Service Balance Top-Up Dialog
            if (uiState.showTopUpModal) {
                ServiceBalanceTopUpDialog(
                    currentBalance = uiState.merchant.serviceBalance,
                    merchant = uiState.merchant,
                    isPremium = uiState.merchant.isPremium,
                    onTopUpSuccess = { amount, method -> viewModel.topUpServiceBalance(amount, method) },
                    onOpenSubscription = { viewModel.showSubscriptionModal(true) },
                    onDismiss = { viewModel.showTopUpModal(false) }
                )
            }

            // Payment Success Fullscreen Overlay
            PaymentSuccessOverlay(
                isVisible = uiState.showSuccessOverlay,
                amount = uiState.successOverlayAmount,
                tipsAmount = uiState.successOverlayTips,
                feeAmount = uiState.successOverlayFee,
                serviceBalance = uiState.merchant.serviceBalance,
                isPremium = uiState.merchant.isPremium,
                businessName = uiState.merchant.businessName,
                onDismiss = { viewModel.dismissSuccessOverlay() }
            )

            // Customer Facing Display Screen (fullscreen overlay)
            if (uiState.isCustomerDisplayOpen) {
                uiState.activePayment?.let { payment ->
                    CustomerFacingScreen(
                        merchant = uiState.merchant,
                        amount = payment.amount,
                        tipsAmount = payment.tipsAmount,
                        qrPayload = if (payment.currentMode == com.example.model.QrPaymentMode.WEB_LINK) payment.bankPaymentUrl else payment.nbuQrPayload,
                        qrBitmap = payment.currentQrBitmap,
                        currentMode = payment.currentMode,
                        itemsSummary = if (payment.cartSnapshot.isNotEmpty())
                            payment.cartSnapshot.joinToString(", ") { "${it.quantity}x ${it.product.name}" }
                        else payment.purpose,
                        status = if (payment.isPaid) TransactionStatus.PAID else TransactionStatus.PENDING,
                        onClose = { viewModel.openCustomerDisplay(false) }
                    )
                }
            }

            // PIN Unlock Dialog
            if (uiState.isPinUnlockDialogOpen) {
                PinUnlockDialog(
                    storedPinHash = uiState.merchant.pinHash,
                    biometricEnabled = uiState.merchant.biometricEnabled,
                    onSuccess = { viewModel.onPinUnlockSuccess() },
                    onDismiss = { viewModel.dismissPinUnlockDialog() }
                )
            }

            // Set PIN Dialog
            if (uiState.isSetPinDialogOpen) {
                SetPinDialog(
                    isCurrentlyProtected = uiState.merchant.isPinProtected,
                    onPinSet = { hash -> viewModel.updatePinHash(hash) },
                    onRemovePin = { viewModel.updatePinHash(null) },
                    onDismiss = { viewModel.showSetPinDialog(false) }
                )
            }

            // Stylish App Entry Splash Animation
            if (isSplashVisible) {
                AppSplashScreen(
                    onSplashFinished = { isSplashVisible = false }
                )
            }
        }
    }
}
