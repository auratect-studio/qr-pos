package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.config.PlatformBillingConfig
import com.example.data.db.AppDatabase
import com.example.data.db.ProductEntity
import com.example.data.db.TransactionEntity
import com.example.data.preferences.MerchantPreferencesStore
import com.example.data.security.SecureTokenStore
import com.example.model.CartItem
import com.example.model.MerchantProfile
import com.example.model.ProductItem
import com.example.model.QrPaymentMode
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.UkrainianBank
import com.example.network.CheckboxApiService
import com.example.network.CheckboxGoodItem
import com.example.network.CheckboxPayment
import com.example.network.CheckboxReceiptRequest
import com.example.network.MonobankApiService
import com.example.network.MonoSetWebhookRequest
import com.example.network.webhook.BankWebhookEvent
import com.example.network.webhook.LivePaymentStreamService
import com.example.network.webhook.StreamConnectionState
import com.example.util.NotificationHelper
import com.example.util.QrGenerator
import com.example.util.ReceiptGenerator
import com.example.service.VoiceCashierHelper
import com.example.service.PaymentSoundHelper
import com.example.data.db.OfflineReceiptEntity
import com.example.worker.OfflineReceiptSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ActivePayment(
    val id: String,
    val amount: Double,
    val tipsAmount: Double = 0.0,
    val bank: UkrainianBank,
    val recipientName: String,
    val iban: String,
    val purpose: String,
    val webQrBitmap: Bitmap?,
    val nbuQrBitmap: Bitmap?,
    val currentMode: QrPaymentMode = QrPaymentMode.WEB_LINK,
    val bankPaymentUrl: String,
    val nbuQrPayload: String,
    val cartSnapshot: List<CartItem>,
    val isPaid: Boolean = false
) {
    val totalAmount: Double get() = amount + tipsAmount

    val currentQrBitmap: Bitmap?
        get() = if (currentMode == QrPaymentMode.WEB_LINK) webQrBitmap else nbuQrBitmap

    val qrBitmap: Bitmap?
        get() = currentQrBitmap
}

data class PosUiState(
    val merchant: MerchantProfile = MerchantProfile(
        businessName = "Малий Богдан Олександрович",
        taxNumber = "3872211492",
        defaultBank = UkrainianBank.PUMB,
        iban = PlatformBillingConfig.MASTER_IBAN
    ),
    val selectedBank: UkrainianBank = UkrainianBank.PUMB,
    val amountInput: String = "",
    val selectedTipAmount: Double = 0.0,
    val paymentPurpose: String = "Оплата за послуги/товари",
    val cartItems: List<CartItem> = emptyList(),
    val productsCatalog: List<ProductItem> = defaultProducts,
    val activePayment: ActivePayment? = null,
    val transactions: List<TransactionRecord> = initialSampleTransactions,
    val inAppBannerNotification: String? = null,
    val showSubscriptionModal: Boolean = false,
    val showProfileModal: Boolean = false,
    val isCustomerDisplayOpen: Boolean = false,
    val showSuccessOverlay: Boolean = false,
    val successOverlayAmount: Double = 0.0,
    val successOverlayTips: Double = 0.0,
    val isPinUnlockDialogOpen: Boolean = false,
    val pendingProtectedAction: (() -> Unit)? = null,
    val isSetPinDialogOpen: Boolean = false,
    val isCheckingMonoPayment: Boolean = false,
    val monoCooldownSeconds: Int = 0,
    val isFiscalizingCheckbox: Boolean = false,
    val lastFiscalCheckCode: String? = null,
    val streamConnectionState: StreamConnectionState = StreamConnectionState.DISCONNECTED,
    val isRegisteringWebhook: Boolean = false,
    val webhookRegisteredSuccessfully: Boolean = false,
    val isAutoFillingFromMono: Boolean = false,
    val showTopUpModal: Boolean = false,
    val lastBalanceDelta: Double? = null,
    val successOverlayFee: Double = 0.0,
    val pendingOfflineReceiptsCount: Int = 0
) {
    val effectiveAmount: Double
        get() {
            return if (cartItems.isNotEmpty()) {
                cartItems.sumOf { it.product.price * it.quantity }
            } else {
                amountInput.toDoubleOrNull() ?: 0.0
            }
        }

    val totalPayableAmount: Double
        get() = effectiveAmount + selectedTipAmount

    val calculatedFee: Double
        get() {
            if (merchant.isPremium) return 0.0
            // 0.8% базова комісія сервісу, 0.5% комісія з чайових
            val baseFee = effectiveAmount * BASE_COMMISSION_RATE
            val tipFee = selectedTipAmount * TIPS_COMMISSION_RATE
            return baseFee + tipFee
        }

    val netToMerchant: Double
        get() = totalPayableAmount - calculatedFee
}

const val BASE_COMMISSION_RATE = PlatformBillingConfig.BASE_COMMISSION_RATE // 0.008 (0.8%)
const val TIPS_COMMISSION_RATE = PlatformBillingConfig.TIPS_COMMISSION_RATE // 0.005 (0.5%)

private val defaultProducts = listOf(
    ProductItem("p1", "Кава фільтр", 65.0, "Кав'ярня", 50),
    ProductItem("p2", "Капучино з вівсяним молоком", 85.0, "Кав'ярня", 40),
    ProductItem("p3", "Круасан мигдалевий", 95.0, "Випічка", 25),
    ProductItem("p4", "Експрес-консультація (30 хв)", 500.0, "Послуги", 10),
    ProductItem("p5", "Манікюр класичний", 450.0, "Краса", 15),
    ProductItem("p6", "Фотосесія експрес (портрет)", 1200.0, "Медіа", 5)
)

private val initialSampleTransactions = listOf(
    TransactionRecord(
        id = "tx-1001",
        amount = 450.0,
        feeAmount = 2.25,
        netAmount = 447.75,
        bank = UkrainianBank.MONOBANK,
        recipientName = "ФОП Шевченко О. В.",
        iban = UkrainianBank.MONOBANK.defaultIban,
        purpose = "Оплата манікюр",
        itemsSummary = "1x Манікюр класичний",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 35,
        status = TransactionStatus.PAID
    ),
    TransactionRecord(
        id = "tx-1002",
        amount = 150.0,
        feeAmount = 0.75,
        netAmount = 149.25,
        bank = UkrainianBank.PRIVATBANK,
        recipientName = "ФОП Шевченко О. В.",
        iban = UkrainianBank.PRIVATBANK.defaultIban,
        purpose = "Оплата кави та випічки",
        itemsSummary = "1x Капучино, 1x Круасан",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 120,
        status = TransactionStatus.PAID
    ),
    TransactionRecord(
        id = "tx-1003",
        amount = 1200.0,
        feeAmount = 6.0,
        netAmount = 1194.0,
        bank = UkrainianBank.MONOBANK,
        recipientName = "ФОП Шевченко О. В.",
        iban = UkrainianBank.MONOBANK.defaultIban,
        purpose = "Оплата фотосесії",
        itemsSummary = "1x Фотосесія експрес",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 26,
        status = TransactionStatus.PAID
    )
)

class PosViewModel @JvmOverloads constructor(
    application: Application,
    private val tokenStore: SecureTokenStore = SecureTokenStore(application),
    private val prefsStore: MerchantPreferencesStore = MerchantPreferencesStore(application),
    private val database: AppDatabase = AppDatabase.getInstance(application),
    val liveStreamService: LivePaymentStreamService = LivePaymentStreamService()
) : AndroidViewModel(application) {

    private val voiceCashier by lazy { VoiceCashierHelper(application) }

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private var lastMonoCheckTimestamp: Long = 0L
    private var monoCooldownJob: Job? = null

    init {
        loadSavedMerchantProfile()
        initDatabaseSync()
        initLivePaymentStreamSync()
    }

    private fun initLivePaymentStreamSync() {
        // 1. Спостереження за станом підключення до SSE-шлюзу
        viewModelScope.launch {
            liveStreamService.connectionState.collect { connState ->
                _uiState.update { it.copy(streamConnectionState = connState) }
            }
        }

        // 2. Спостереження за вхідними подіями платежів від банку в реальному часі
        viewModelScope.launch {
            liveStreamService.events.collect { event ->
                handleIncomingWebhookEvent(event)
            }
        }
    }

    private fun initDatabaseSync() {
        // 1. Ініціалізація та спостереження за каталогом товарів
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = database.productDao().getProductCount()
                if (count == 0) {
                    database.productDao().insertAll(defaultProducts.map { ProductEntity.fromModel(it) })
                }
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                database.productDao().getAllProductsFlow().collect { entities ->
                    if (entities.isNotEmpty()) {
                        _uiState.update { it.copy(productsCatalog = entities.map { e -> e.toModel() }) }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Ініціалізація та спостереження за історією транзакцій
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val txCount = database.transactionDao().getTransactionCount()
                if (txCount == 0) {
                    database.transactionDao().insertAll(initialSampleTransactions.map { TransactionEntity.fromModel(it) })
                }
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            try {
                database.transactionDao().getAllTransactionsFlow().collect { entities ->
                    if (entities.isNotEmpty()) {
                        _uiState.update { it.copy(transactions = entities.map { e -> e.toModel() }) }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Спостереження за чергою офлайн-буферизованих чеків
        viewModelScope.launch {
            try {
                database.offlineReceiptDao().getPendingCountFlow().collect { pendingCount ->
                    _uiState.update { it.copy(pendingOfflineReceiptsCount = pendingCount) }
                }
            } catch (_: Exception) {}
        }

        // Автоматичний запуск фонової синхронізації накопичених чеків при старті застосунку
        try {
            OfflineReceiptSyncWorker.enqueueSync(getApplication())
        } catch (_: Exception) {}
    }

    fun loadSavedMerchantProfile(): Job {
        return viewModelScope.launch {
            try {
                val savedProfile = prefsStore.getSavedProfile()
                val monoToken = tokenStore.getMonobankToken()
                val checkboxToken = tokenStore.getCheckboxToken()

                val fullProfile = if (savedProfile != null) {
                    savedProfile.copy(
                        monobankToken = monoToken,
                        checkboxApiKey = checkboxToken
                    )
                } else {
                    MerchantProfile(
                        monobankToken = monoToken,
                        checkboxApiKey = checkboxToken
                    )
                }

                _uiState.update { current ->
                    current.copy(
                        merchant = fullProfile,
                        selectedBank = fullProfile.defaultBank
                    )
                }

                // Запускаємо прослуховування Live SSE потоку з параметрами мерчанта
                liveStreamService.startStreaming(
                    gatewayUrl = fullProfile.webhookGatewayUrl,
                    merchantId = fullProfile.merchantId
                )
            } catch (e: Exception) {
                // Graceful fallback for first launch or missing storage
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveStreamService.stopStreaming()
        try {
            voiceCashier.shutdown()
        } catch (_: Exception) {}
    }

    fun selectBank(bank: UkrainianBank) {
        _uiState.update { current ->
            val allDefaultIbans = UkrainianBank.entries.map { it.defaultIban }.toSet()
            val currentIban = current.merchant.iban.trim()
            val isCustomIban = currentIban.isNotBlank() && !allDefaultIbans.contains(currentIban)

            current.copy(
                selectedBank = bank,
                merchant = current.merchant.copy(
                    defaultBank = bank,
                    iban = if (isCustomIban) currentIban else bank.defaultIban
                )
            )
        }
    }

    fun onKeypadInput(key: String) {
        if (_uiState.value.cartItems.isNotEmpty()) return

        _uiState.update { current ->
            val currentStr = current.amountInput
            val newStr = when (key) {
                "CLEAR" -> ""
                "DEL" -> if (currentStr.isNotEmpty()) currentStr.dropLast(1) else ""
                "." -> {
                    if (currentStr.isEmpty()) "0."
                    else if (!currentStr.contains(".")) "."
                    else currentStr
                }
                else -> {
                    if (currentStr.contains(".")) {
                        val parts = currentStr.split(".")
                        if (parts.size > 1 && parts[1].length >= 2) currentStr
                        else currentStr + key
                    } else if (currentStr == "0") {
                        key
                    } else if (currentStr.length < 7) {
                        currentStr + key
                    } else {
                        currentStr
                    }
                }
            }
            current.copy(amountInput = newStr)
        }
    }

    fun setQuickAmount(amount: Double) {
        _uiState.update { current ->
            current.copy(
                cartItems = emptyList(),
                amountInput = String.format(java.util.Locale.US, "%.0f", amount)
            )
        }
    }

    fun updatePurpose(purpose: String) {
        _uiState.update { it.copy(paymentPurpose = purpose) }
    }

    fun addToCart(product: ProductItem) {
        _uiState.update { current ->
            val existing = current.cartItems.find { it.product.id == product.id }
            val updated = if (existing != null) {
                current.cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                current.cartItems + CartItem(product, 1)
            }
            current.copy(cartItems = updated, amountInput = "")
        }
    }

    fun removeFromCart(product: ProductItem) {
        _uiState.update { current ->
            val existing = current.cartItems.find { it.product.id == product.id }
            val updated = if (existing != null && existing.quantity > 1) {
                current.cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity - 1) else it
                }
            } else {
                current.cartItems.filterNot { it.product.id == product.id }
            }
            current.copy(cartItems = updated)
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }

    fun addNewProduct(name: String, price: Double, category: String, stock: Int) {
        val newProduct = ProductItem(
            id = "prod-",
            name = name,
            price = price,
            category = category,
            stock = stock
        )
        // Збереження у локальну базу даних Room
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.productDao().insertProduct(ProductEntity.fromModel(newProduct))
            } catch (_: Exception) {}
        }
        _uiState.update { current ->
            current.copy(productsCatalog = listOf(newProduct) + current.productsCatalog)
        }
    }

    fun setTipAmount(tip: Double) {
        _uiState.update { current ->
            val active = current.activePayment
            val updated = current.copy(selectedTipAmount = tip)
            if (active != null) {
                val total = active.amount + tip
                val bankUrl = QrGenerator.buildBankPaymentUrl(
                    bank = active.bank,
                    iban = active.iban,
                    amount = total,
                    purpose = active.purpose,
                    recipientName = active.recipientName,
                    gatewayBaseUrl = current.merchant.webhookGatewayUrl,
                    cardNumber = current.merchant.cardNumber
                )
                val nbuPayload = QrGenerator.buildNbuQrPayload(active.recipientName, active.iban, total, active.purpose)
                val webQrBitmap = QrGenerator.generateQrBitmap(bankUrl, sizePx = 600)
                val nbuQrBitmap = QrGenerator.generateQrBitmap(nbuPayload, sizePx = 600)
                updated.copy(
                    activePayment = active.copy(
                        tipsAmount = tip,
                        webQrBitmap = webQrBitmap,
                        nbuQrBitmap = nbuQrBitmap,
                        bankPaymentUrl = bankUrl,
                        nbuQrPayload = nbuPayload
                    )
                )
            } else {
                updated
            }
        }
    }

    fun generatePayment() {
        val state = _uiState.value
        val amount = state.effectiveAmount
        if (amount <= 0.0) return

        val bank = state.selectedBank
        val iban = state.merchant.iban
        val recipient = state.merchant.businessName.ifBlank { "ФОП Одержувач" }
        val tip = state.selectedTipAmount
        val total = amount + tip
        val purpose = if (state.cartItems.isNotEmpty()) {
            "Оплата: " + state.cartItems.joinToString(", ") { "${it.quantity}x ${it.product.name}" }
        } else {
            state.paymentPurpose
        }

        val bankUrl = QrGenerator.buildBankPaymentUrl(
            bank = bank,
            iban = iban,
            amount = total,
            purpose = purpose,
            recipientName = recipient,
            gatewayBaseUrl = state.merchant.webhookGatewayUrl,
            cardNumber = state.merchant.cardNumber
        )
        val nbuPayload = QrGenerator.buildNbuQrPayload(recipient, iban, total, purpose)

        // Генеруємо обидва варіанти QR (для швидкого миттєвого перемикання тумблером)
        val webQrBitmap = QrGenerator.generateQrBitmap(bankUrl, sizePx = 600)
        val nbuQrBitmap = QrGenerator.generateQrBitmap(nbuPayload, sizePx = 600)

        val payment = ActivePayment(
            id = "tx-${UUID.randomUUID().toString().take(8)}",
            amount = amount,
            tipsAmount = tip,
            bank = bank,
            recipientName = recipient,
            iban = iban,
            purpose = purpose,
            webQrBitmap = webQrBitmap,
            nbuQrBitmap = nbuQrBitmap,
            currentMode = QrPaymentMode.WEB_LINK,
            bankPaymentUrl = bankUrl,
            nbuQrPayload = nbuPayload,
            cartSnapshot = state.cartItems
        )

        _uiState.update { it.copy(activePayment = payment) }
    }

    fun setQrPaymentMode(mode: QrPaymentMode) {
        _uiState.update { current ->
            val active = current.activePayment ?: return@update current
            current.copy(activePayment = active.copy(currentMode = mode))
        }
    }

    fun dismissPayment() {
        _uiState.update { it.copy(activePayment = null, selectedTipAmount = 0.0) }
    }

    fun simulateCustomerPayment(context: Context) {
        val active = _uiState.value.activePayment ?: return
        val isPremium = _uiState.value.merchant.isPremium
        val baseFee = if (isPremium) 0.0 else (active.amount * BASE_COMMISSION_RATE)
        val tipsFee = if (isPremium) 0.0 else (active.tipsAmount * TIPS_COMMISSION_RATE)
        val totalFee = baseFee + tipsFee
        val totalPaid = active.totalAmount
        val net = totalPaid - totalFee

        val record = TransactionRecord(
            id = active.id,
            amount = totalPaid,
            tipsAmount = active.tipsAmount,
            feeAmount = totalFee,
            netAmount = net,
            bank = active.bank,
            recipientName = active.recipientName,
            iban = active.iban,
            purpose = active.purpose,
            itemsSummary = if (active.cartSnapshot.isNotEmpty()) {
                active.cartSnapshot.joinToString(", ") { "${it.quantity}x ${it.product.name}" }
            } else {
                active.purpose
            },
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.PAID
        )

        // Зберігаємо транзакцію в базі даних Room
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.transactionDao().insertTransaction(TransactionEntity.fromModel(record))
            } catch (_: Exception) {}
        }

        // Автоматична буферизація чеку для офлайн-синхронізації / ПРРО
        if (_uiState.value.merchant.autoFiscalizeWithCheckbox) {
            bufferOfflineReceipt(record)
        }

        // Голосовий касир (PRO) - озвучує суму після появи оверлею та завершення касового дзвону
        if (isPremium && _uiState.value.merchant.voiceCashierEnabled) {
            viewModelScope.launch {
                delay(450)
                try {
                    voiceCashier.announcePayment(totalPaid)
                } catch (_: Exception) {}
            }
        }

        // Показуємо push-сповіщення
        NotificationHelper.showPaymentReceivedNotification(
            context = context,
            amount = totalPaid,
            bankName = active.bank.shortName,
            transactionId = active.id
        )

        var updatedMerchant = _uiState.value.merchant
        if (!isPremium && totalFee > 0.0) {
            val newBalance = updatedMerchant.serviceBalance - totalFee
            updatedMerchant = updatedMerchant.copy(serviceBalance = newBalance)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    prefsStore.saveProfile(updatedMerchant)
                } catch (_: Exception) {}
            }
        }

        val formattedAmount = String.format(java.util.Locale.US, "%.2f", totalPaid)
        val feeMsg = if (!isPremium && totalFee > 0.0) {
            " (комісія: –${String.format(java.util.Locale.US, "%.2f", totalFee)} ₴, баланс: ${String.format(java.util.Locale.US, "%.2f", updatedMerchant.serviceBalance)} ₴)"
        } else ""
        val bannerMsg = "✅ Зараховано +$formattedAmount ₴ на ${active.bank.shortName}!$feeMsg"

        _uiState.update { current ->
            current.copy(
                merchant = updatedMerchant,
                lastBalanceDelta = if (!isPremium && totalFee > 0.0) -totalFee else null,
                activePayment = active.copy(isPaid = true),
                transactions = listOf(record) + current.transactions,
                inAppBannerNotification = bannerMsg,
                amountInput = "",
                selectedTipAmount = 0.0,
                cartItems = emptyList(),
                showSuccessOverlay = true,
                successOverlayAmount = totalPaid,
                successOverlayTips = active.tipsAmount,
                successOverlayFee = totalFee
            )
        }
    }

    fun showTopUpModal(show: Boolean) {
        _uiState.update { it.copy(showTopUpModal = show) }
    }

    fun clearBalanceDelta() {
        _uiState.update { it.copy(lastBalanceDelta = null) }
    }

    fun topUpServiceBalance(amount: Double, paymentMethod: String = "Google Pay") {
        val currentBalance = _uiState.value.merchant.serviceBalance
        val newBalance = currentBalance + amount
        val updated = _uiState.value.merchant.copy(serviceBalance = newBalance)
        _uiState.update {
            it.copy(
                merchant = updated,
                lastBalanceDelta = amount,
                showTopUpModal = false,
                inAppBannerNotification = "✅ Баланс поповнено на +${String.format(java.util.Locale.US, "%.2f", amount)} ₴! Доступно: ${String.format(java.util.Locale.US, "%.2f", newBalance)} ₴"
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                prefsStore.saveProfile(updated)
            } catch (_: Exception) {}
        }
    }

    fun clearBanner() {
        _uiState.update { it.copy(inAppBannerNotification = null) }
    }

    fun togglePremium() {
        _uiState.update { current ->
            val updatedPremium = !current.merchant.isPremium
            val updatedMerchant = current.merchant.copy(isPremium = updatedPremium)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    prefsStore.saveProfile(updatedMerchant)
                } catch (_: Exception) {}
            }
            current.copy(
                merchant = updatedMerchant,
                showSubscriptionModal = false
            )
        }
    }

    fun showSubscriptionModal(show: Boolean) {
        _uiState.update { it.copy(showSubscriptionModal = show) }
    }

    fun showProfileModal(show: Boolean) {
        _uiState.update { it.copy(showProfileModal = show) }
    }

    fun updateMerchantProfile(updatedProfile: MerchantProfile): Job {
        _uiState.update { current ->
            current.copy(
                merchant = updatedProfile,
                selectedBank = updatedProfile.defaultBank,
                showProfileModal = false,
                inAppBannerNotification = "✅ Профіль ${updatedProfile.businessName} збережено!"
            )
        }

        // Перезапускаємо SSE потік для оновленого шлюзу або ID мерчанта
        liveStreamService.startStreaming(
            gatewayUrl = updatedProfile.webhookGatewayUrl,
            merchantId = updatedProfile.merchantId,
            forceReconnect = true
        )

        return viewModelScope.launch(Dispatchers.IO) {
            try {
                tokenStore.saveTokens(
                    monobankToken = updatedProfile.monobankToken,
                    checkboxToken = updatedProfile.checkboxApiKey
                )
                prefsStore.saveProfile(updatedProfile)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(inAppBannerNotification = "⚠️ Помилка збереження налаштувань: ${e.message}")
                }
            }
        }
    }

    fun clearSecureTokens() {
        tokenStore.clearTokens()
        _uiState.update { current ->
            current.copy(
                merchant = current.merchant.copy(
                    monobankToken = "",
                    checkboxApiKey = ""
                ),
                inAppBannerNotification = "🔒 Збережені токени API успішно видалено з Keystore"
            )
        }
    }

    fun resetMerchantProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tokenStore.clearTokens()
                prefsStore.clearProfile()
                _uiState.update { current ->
                    current.copy(
                        merchant = MerchantProfile(),
                        showProfileModal = false,
                        inAppBannerNotification = "ℹ️ Профіль мерчанта скинуто до початкового стану"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(inAppBannerNotification = "⚠️ Помилка скидання: ${e.message}")
                }
            }
        }
    }

    private val monoApi by lazy { MonobankApiService.create() }
    private val checkboxApi by lazy { CheckboxApiService.create() }

    /**
     * Обробка вхідної події платежу від банку через SSE Webhook Gateway.
     * Працює миттєво в реальному часі без опитування API.
     */
    fun handleIncomingWebhookEvent(event: BankWebhookEvent) {
        val active = _uiState.value.activePayment
        val matchingByTotal = active != null && !active.isPaid && Math.abs(active.totalAmount - event.amount) < 0.01
        val matchingByBase = active != null && !active.isPaid && Math.abs(active.amount - event.amount) < 0.01
        val isMatchingActive = matchingByTotal || matchingByBase

        val isPremium = _uiState.value.merchant.isPremium
        val tipsAmount = if (matchingByTotal) active?.tipsAmount ?: 0.0 else 0.0
        val baseAmount = event.amount - tipsAmount
        val baseFee = if (isPremium) 0.0 else (baseAmount * BASE_COMMISSION_RATE)
        val tipsFee = if (isPremium) 0.0 else (tipsAmount * TIPS_COMMISSION_RATE)
        val totalFee = baseFee + tipsFee
        val net = event.amount - totalFee
        val txId = event.transactionId.ifBlank { "wh-${System.currentTimeMillis()}" }

        val bankMatch = UkrainianBank.entries.firstOrNull {
            it.name.equals(event.bank, ignoreCase = true) ||
            it.shortName.equals(event.bank, ignoreCase = true) ||
            it.id.equals(event.bank, ignoreCase = true)
        } ?: UkrainianBank.MONOBANK

        val record = TransactionRecord(
            id = txId,
            amount = event.amount,
            tipsAmount = tipsAmount,
            feeAmount = totalFee,
            netAmount = net,
            bank = bankMatch,
            recipientName = active?.recipientName ?: _uiState.value.merchant.businessName,
            iban = active?.iban ?: _uiState.value.merchant.iban,
            purpose = if (event.comment.isNotBlank()) event.comment else (active?.purpose ?: "Оплата через Webhook"),
            itemsSummary = if (active != null && active.cartSnapshot.isNotEmpty()) {
                active.cartSnapshot.joinToString(", ") { "${it.quantity}x ${it.product.name}" }
            } else {
                "Оплата через ${bankMatch.shortName}"
            },
            timestamp = event.timestamp,
            status = TransactionStatus.PAID
        )

        // Зберігаємо транзакцію в базі даних Room
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.transactionDao().insertTransaction(TransactionEntity.fromModel(record))
            } catch (_: Exception) {}
        }

        // Автоматична буферизація чеку для офлайн-синхронізації / ПРРО
        if (_uiState.value.merchant.autoFiscalizeWithCheckbox) {
            bufferOfflineReceipt(record)
        }

        // Голосовий касир (PRO) - озвучує суму після появи оверлею та завершення касового дзвону
        if (isPremium && _uiState.value.merchant.voiceCashierEnabled) {
            viewModelScope.launch {
                delay(450)
                try {
                    voiceCashier.announcePayment(event.amount)
                } catch (_: Exception) {}
            }
        }

        // Показуємо push-сповіщення
        try {
            NotificationHelper.showPaymentReceivedNotification(
                context = getApplication(),
                amount = event.amount,
                bankName = bankMatch.shortName,
                transactionId = txId
            )
        } catch (_: Exception) {}

        var updatedMerchant = _uiState.value.merchant
        if (!isPremium && totalFee > 0.0) {
            val newBalance = updatedMerchant.serviceBalance - totalFee
            updatedMerchant = updatedMerchant.copy(serviceBalance = newBalance)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    prefsStore.saveProfile(updatedMerchant)
                } catch (_: Exception) {}
            }
        }

        val formattedAmount = String.format(java.util.Locale.US, "%.2f", event.amount)
        val feeMsg = if (!isPremium && totalFee > 0.0) {
            " (комісія: –${String.format(java.util.Locale.US, "%.2f", totalFee)} ₴, баланс: ${String.format(java.util.Locale.US, "%.2f", updatedMerchant.serviceBalance)} ₴)"
        } else ""
        val bannerMsg = "🎉 Webhook: Зараховано +$formattedAmount ₴ від ${bankMatch.shortName}!$feeMsg"

        _uiState.update { current ->
            current.copy(
                merchant = updatedMerchant,
                lastBalanceDelta = if (!isPremium && totalFee > 0.0) -totalFee else null,
                activePayment = if (isMatchingActive) active?.copy(isPaid = true) else current.activePayment,
                transactions = listOf(record) + current.transactions.filterNot { it.id == record.id },
                inAppBannerNotification = bannerMsg,
                amountInput = if (isMatchingActive) "" else current.amountInput,
                selectedTipAmount = if (isMatchingActive) 0.0 else current.selectedTipAmount,
                cartItems = if (isMatchingActive) emptyList() else current.cartItems,
                showSuccessOverlay = true,
                successOverlayAmount = event.amount,
                successOverlayTips = tipsAmount,
                successOverlayFee = totalFee
            )
        }
    }

    /**
     * Реєстрація Webhook-шлюзу у Monobank API.
     */
    fun registerMonobankWebhook(context: Context) {
        val merchant = _uiState.value.merchant
        val token = merchant.monobankToken.trim()
        if (token.isBlank()) {
            _uiState.update {
                it.copy(inAppBannerNotification = "ℹ️ Спочатку додайте Monobank X-Token у розділі 'Акаунт та реквізити'!")
            }
            return
        }

        val webhookUrl = "${merchant.webhookGatewayUrl.trimEnd('/')}/webhook/mono/${merchant.merchantId}"
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRegisteringWebhook = true) }
            try {
                val response = monoApi.setWebhook(token = token, body = MonoSetWebhookRequest(webHookUrl = webhookUrl))
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            webhookRegisteredSuccessfully = true,
                            inAppBannerNotification = "✅ Webhook успішно зареєстровано в Monobank API! Усі зарахування надходитимуть миттєво."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            inAppBannerNotification = "⚠️ Monobank повернув помилку ${response.code()} при реєстрації Webhook URL: $webhookUrl"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(inAppBannerNotification = "⚠️ Помилка реєстрації Webhook: ${e.message}")
                }
            } finally {
                _uiState.update { it.copy(isRegisteringWebhook = false) }
            }
        }
    }

    /**
     * 🪄 Автозаповнення реквізитів з Monobank API за 1 клік.
     * Отримує офіційне ім'я, розрахунковий рахунок IBAN (ФОП/UAH), номер картки та автоматично реєструє Webhook!
     */
    fun autoFillFromMonobank(token: String, onResult: ((Boolean, String) -> Unit)? = null) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            _uiState.update {
                it.copy(inAppBannerNotification = "ℹ️ Спочатку вкажіть або вставте з буфера Monobank X-Token")
            }
            onResult?.invoke(false, "Токен порожній")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isAutoFillingFromMono = true) }
            try {
                val clientInfo = monoApi.getClientInfo(token = cleanToken)
                val clientName = clientInfo.name.trim()

                // Пріоритет: 1) ФОП рахунок, 2) Гривневий рахунок (980), 3) Будь-який з IBAN
                val fopAccount = clientInfo.accounts.firstOrNull { it.type.equals("fop", ignoreCase = true) && !it.iban.isNullOrBlank() }
                val uahAccount = clientInfo.accounts.firstOrNull { it.currencyCode == 980 && !it.iban.isNullOrBlank() }
                val selectedAccount = fopAccount ?: uahAccount ?: clientInfo.accounts.firstOrNull { !it.iban.isNullOrBlank() }

                val resolvedIban = selectedAccount?.iban?.ifBlank { null } ?: _uiState.value.merchant.iban
                val resolvedCard = selectedAccount?.maskedPan?.firstOrNull()?.ifBlank { null } ?: _uiState.value.merchant.cardNumber

                val updatedProfile = _uiState.value.merchant.copy(
                    businessName = if (clientName.isNotBlank()) clientName else _uiState.value.merchant.businessName,
                    iban = resolvedIban,
                    cardNumber = resolvedCard,
                    monobankToken = cleanToken,
                    defaultBank = UkrainianBank.MONOBANK
                )

                // Зберігаємо у Keystore та SharedPreferences
                tokenStore.saveTokens(
                    monobankToken = cleanToken,
                    checkboxToken = updatedProfile.checkboxApiKey
                )
                prefsStore.saveProfile(updatedProfile)

                // Одночасно автоматично підключаємо Webhook у банку
                try {
                    val webhookUrl = "${updatedProfile.webhookGatewayUrl.trimEnd('/')}/webhook/mono/${updatedProfile.merchantId}"
                    monoApi.setWebhook(token = cleanToken, body = MonoSetWebhookRequest(webHookUrl = webhookUrl))
                } catch (_: Exception) {}

                // Запускаємо фоновий SSE-потік
                liveStreamService.startStreaming(
                    gatewayUrl = updatedProfile.webhookGatewayUrl,
                    merchantId = updatedProfile.merchantId
                )

                val successMsg = "🪄 Реквізити імпортовано з Monobank! IBAN: ${resolvedIban.take(14)}..."
                _uiState.update {
                    it.copy(
                        merchant = updatedProfile,
                        selectedBank = UkrainianBank.MONOBANK,
                        inAppBannerNotification = "✅ $successMsg",
                        isAutoFillingFromMono = false,
                        webhookRegisteredSuccessfully = true
                    )
                }
                onResult?.invoke(true, successMsg)
            } catch (e: Exception) {
                val errMsg = if (e is retrofit2.HttpException && e.code() == 403) {
                    "⚠️ Невірний X-Token Monobank або вичерпано термін його дії."
                } else if (e is retrofit2.HttpException && e.code() == 429) {
                    "⏳ Ліміт запитів Monobank (1 запит/хв). Зачекайте 1 хвилину."
                } else {
                    "⚠️ Помилка з'єднання з Monobank: ${e.message}"
                }
                _uiState.update {
                    it.copy(
                        isAutoFillingFromMono = false,
                        inAppBannerNotification = errMsg
                    )
                }
                onResult?.invoke(false, errMsg)
            }
        }
    }

    /**
     * Відправка симуляційного тестового платежу через шлюз.
     */
    fun triggerTestWebhookPayment() {
        viewModelScope.launch(Dispatchers.IO) {
            val active = _uiState.value.activePayment
            val amount = active?.amount ?: 150.0
            val bank = active?.bank?.shortName ?: "Monobank"
            val success = liveStreamService.triggerTestPayment(
                amount = amount,
                bank = bank,
                comment = "Тестове зарахування через Webhook Gateway"
            )
            if (!success) {
                _uiState.update {
                    it.copy(inAppBannerNotification = "ℹ️ Тестовий шлюз офлайн (${liveStreamService.currentGatewayUrl}). Пряма перевірка в банку активна.")
                }
            }
        }
    }

    /**
     * Повторна спроба підключення до SSE-шлюзу за запитом користувача.
     */
    fun retryGatewayConnection() {
        liveStreamService.reconnect()
    }

    /**
     * Ручна перевірка надходження коштів через Monobank API із захистом від повторної активації (антиспам 10 сек).
     */
    fun checkMonobankLivePayment(context: Context) {
        val active = _uiState.value.activePayment ?: return
        val token = _uiState.value.merchant.monobankToken.trim()

        if (token.isBlank()) {
            _uiState.update {
                it.copy(inAppBannerNotification = "ℹ️ Вкажіть токен Monobank API в налаштуваннях акаунта для живої синхронізації!")
            }
            return
        }

        val now = System.currentTimeMillis()
        val elapsedSinceLast = (now - lastMonoCheckTimestamp) / 1000
        if (elapsedSinceLast < 10 && lastMonoCheckTimestamp > 0) {
            val remaining = (10 - elapsedSinceLast).toInt()
            _uiState.update {
                it.copy(
                    inAppBannerNotification = "⏳ Захист від повторної активації: зачекайте ще $remaining сек."
                )
            }
            return
        }

        lastMonoCheckTimestamp = now
        startMonoCooldownTimer(10)

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isCheckingMonoPayment = true) }
            try {
                // Query statements from last 10 minutes (600 seconds)
                val fromTime = (System.currentTimeMillis() / 1000) - 600
                val statement = monoApi.getStatement(token = token, account = "0", fromTimestampSeconds = fromTime)

                val expectedKopecks = Math.round(active.amount * 100)
                val matched = statement.firstOrNull { it.amount > 0 && it.amount == expectedKopecks }

                if (matched != null) {
                    launch(Dispatchers.Main) {
                        simulateCustomerPayment(context)
                        _uiState.update {
                            it.copy(inAppBannerNotification = "🎉 Monobank API підтвердив надходження +${active.amount} ₴!")
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckingMonoPayment = false,
                            inAppBannerNotification = "⏳ Нових надходжень на суму ${active.amount} ₴ у Monobank поки не знайдено."
                        )
                    }
                }
            } catch (e: Exception) {
                val is429 = (e is retrofit2.HttpException && e.code() == 429) || e.message?.contains("429") == true
                val errorMsg = if (is429) {
                    "⚠️ Перевищено ліміт запитів Monobank (429 Too Many Requests). Будь ласка, зачекайте або скористайтеся Live Webhook."
                } else {
                    "⚠️ Помилка з'єднання з Monobank API: ${e.message}"
                }
                _uiState.update {
                    it.copy(
                        isCheckingMonoPayment = false,
                        inAppBannerNotification = errorMsg
                    )
                }
            } finally {
                _uiState.update { it.copy(isCheckingMonoPayment = false) }
            }
        }
    }

    private fun startMonoCooldownTimer(seconds: Int) {
        monoCooldownJob?.cancel()
        monoCooldownJob = viewModelScope.launch {
            for (sec in seconds downTo 1) {
                _uiState.update { it.copy(monoCooldownSeconds = sec) }
                delay(1000)
            }
            _uiState.update { it.copy(monoCooldownSeconds = 0) }
        }
    }

    fun fiscalizeWithCheckbox(record: TransactionRecord) {
        val apiKey = _uiState.value.merchant.checkboxApiKey.trim()
        if (apiKey.isBlank()) {
            _uiState.update {
                it.copy(inAppBannerNotification = "ℹ️ Для фіскалізації додайте Ключ касира Checkbox у налаштуваннях акаунта.")
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isFiscalizingCheckbox = true) }
            try {
                val goods = if (_uiState.value.cartItems.isNotEmpty()) {
                    _uiState.value.cartItems.map {
                        CheckboxGoodItem(
                            name = it.product.name,
                            code = it.product.id,
                            priceKopecks = Math.round(it.product.price * 100),
                            quantityThousandths = it.quantity * 1000L
                        )
                    }
                } else {
                    listOf(
                        CheckboxGoodItem(
                            name = record.itemsSummary.ifBlank { "Послуги / Товари" },
                            code = "ITEM-001",
                            priceKopecks = Math.round(record.amount * 100),
                            quantityThousandths = 1000L
                        )
                    )
                }

                val goodsList = goods.toMutableList()
                if (record.tipsAmount > 0.0) {
                    goodsList.add(
                        CheckboxGoodItem(
                            name = "Чайові персоналу",
                            code = "TIPS",
                            priceKopecks = Math.round(record.tipsAmount * 100),
                            quantityThousandths = 1000L
                        )
                    )
                }

                val totalKopecks = Math.round((record.amount + record.tipsAmount) * 100)
                val req = CheckboxReceiptRequest(
                    goods = goodsList,
                    payments = listOf(
                        CheckboxPayment(
                            valueKopecks = totalKopecks,
                            label = "QR-код (${record.bank.shortName})"
                        )
                    )
                )

                val bearer = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
                val response = checkboxApi.createSellReceipt(bearerToken = bearer, request = req)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val fiscalCode = body.fiscalCode ?: body.id.take(10)
                    try {
                        database.offlineReceiptDao().markAsSynced(record.id, fiscalCode, body.taxUrl)
                    } catch (_: Exception) {}

                    _uiState.update {
                        it.copy(
                            lastFiscalCheckCode = fiscalCode,
                            inAppBannerNotification = "🧾 Чек успішно фіскалізовано в ДПС! Номер: $fiscalCode"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            inAppBannerNotification = "⚠️ ПРРО Checkbox повернув код ${response.code()}: перевірте ключ касира."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        inAppBannerNotification = "⚠️ Помилка зв'язку з сервером Checkbox: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isFiscalizingCheckbox = false) }
            }
        }
    }

    fun getReceiptText(transaction: TransactionRecord): String {
        val currentMerchant = _uiState.value.merchant
        return ReceiptGenerator.generateReceiptText(
            transaction = transaction,
            taxNumber = currentMerchant.taxNumber,
            merchant = currentMerchant
        )
    }

    fun bufferOfflineReceipt(record: TransactionRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val itemsJson = if (_uiState.value.cartItems.isNotEmpty()) {
                    val arr = org.json.JSONArray()
                    _uiState.value.cartItems.forEach { item ->
                        val obj = org.json.JSONObject()
                        obj.put("name", item.product.name)
                        obj.put("code", item.product.id)
                        obj.put("price", item.product.price)
                        obj.put("quantity", item.quantity)
                        arr.put(obj)
                    }
                    arr.toString()
                } else {
                    record.itemsSummary
                }

                val receiptEntity = OfflineReceiptEntity(
                    id = record.id,
                    transactionId = record.id,
                    amount = record.amount,
                    tipsAmount = record.tipsAmount,
                    itemsJson = itemsJson,
                    merchantTaxNumber = _uiState.value.merchant.taxNumber,
                    createdAt = record.timestamp
                )
                database.offlineReceiptDao().insertReceipt(receiptEntity)
                OfflineReceiptSyncWorker.enqueueSync(getApplication())
            } catch (_: Exception) {}
        }
    }

    fun syncOfflineReceiptsNow() {
        viewModelScope.launch(Dispatchers.IO) {
            OfflineReceiptSyncWorker.enqueueSync(getApplication())
            val count = database.offlineReceiptDao().getPendingCount()
            _uiState.update {
                it.copy(
                    inAppBannerNotification = if (count > 0)
                        "🔄 Запущено фонову синхронізацію $count офлайн-чеків із Checkbox..."
                    else
                        "✅ Усі чеки синхронізовані. Офлайн-черга порожня."
                )
            }
        }
    }

    fun openCustomerDisplay(open: Boolean) {
        _uiState.update { it.copy(isCustomerDisplayOpen = open) }
    }

    fun dismissSuccessOverlay() {
        _uiState.update { it.copy(showSuccessOverlay = false) }
    }

    fun requestProtectedAction(action: () -> Unit) {
        val pinHash = _uiState.value.merchant.pinHash
        if (pinHash.isBlank()) {
            action()
        } else {
            _uiState.update {
                it.copy(
                    isPinUnlockDialogOpen = true,
                    pendingProtectedAction = action
                )
            }
        }
    }

    fun onPinUnlockSuccess() {
        val pending = _uiState.value.pendingProtectedAction
        _uiState.update {
            it.copy(
                isPinUnlockDialogOpen = false,
                pendingProtectedAction = null
            )
        }
        pending?.invoke()
    }

    fun dismissPinUnlockDialog() {
        _uiState.update {
            it.copy(
                isPinUnlockDialogOpen = false,
                pendingProtectedAction = null
            )
        }
    }

    fun showSetPinDialog(show: Boolean) {
        _uiState.update { it.copy(isSetPinDialogOpen = show) }
    }

    fun updatePinHash(newHash: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _uiState.value.merchant.copy(
                pinHash = newHash ?: "",
                biometricEnabled = newHash != null && _uiState.value.merchant.biometricEnabled
            )
            prefsStore.saveProfile(updated)
            _uiState.update {
                it.copy(
                    merchant = updated,
                    isSetPinDialogOpen = false,
                    inAppBannerNotification = if (!newHash.isNullOrBlank()) "🔒 PIN-код успішно встановлено!" else "🔓 PIN-код вимкнено"
                )
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _uiState.value.merchant.copy(biometricEnabled = enabled)
            prefsStore.saveProfile(updated)
            _uiState.update { it.copy(merchant = updated) }
        }
    }

    fun toggleVoiceCashier(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _uiState.value.merchant.copy(voiceCashierEnabled = enabled)
            prefsStore.saveProfile(updated)
            _uiState.update { it.copy(merchant = updated) }
        }
    }
}