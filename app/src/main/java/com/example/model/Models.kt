package com.example.model

enum class UkrainianBank(
    val id: String,
    val displayName: String,
    val shortName: String,
    val defaultIban: String,
    val defaultCardNumber: String,
    val brandColorHex: Long,
    val secondaryColorHex: Long,
    /** URI-схема для прямого відкриття мобільного додатку банку (якщо встановлений) */
    val deepLinkScheme: String,
    /** Веб-сторінка оплати банку як резервний варіант */
    val webFallbackUrl: String,
    /** true — банк має справжній P2P web-URL (App Link), false — тільки deep-link або сайт */
    val hasNativeP2pLink: Boolean,
    /** Android Package Name для прямого запуску встановленого застосунку банку */
    val packageName: String
) {
    MONOBANK(
        id = "mono",
        displayName = "Monobank (АТ «Універсал Банк»)",
        shortName = "Monobank",
        defaultIban = "UA213220010000026007234567890",
        defaultCardNumber = "4441 1144 5566 7788",
        brandColorHex = 0xFF18181B,
        secondaryColorHex = 0xFFE11D48,
        deepLinkScheme = "monobank://",
        webFallbackUrl = "https://send.monobank.ua",
        hasNativeP2pLink = true,
        packageName = "com.ftband.mono"
    ),
    PRIVATBANK(
        id = "privat",
        displayName = "ПриватБанк (Privat24)",
        shortName = "ПриватБанк",
        defaultIban = "UA123052990000026001234567890",
        defaultCardNumber = "5168 7573 1234 5678",
        brandColorHex = 0xFF15803D,
        secondaryColorHex = 0xFF86EFAC,
        deepLinkScheme = "privat24://",
        webFallbackUrl = "https://next.privat24.ua/money-transfer/card",
        hasNativeP2pLink = true,
        packageName = "ua.privatbank.ap24"
    ),
    PUMB(
        id = "pumb",
        displayName = "ПУМБ (Перший Українсь. Міжнар. Банк)",
        shortName = "ПУМБ",
        defaultIban = "UA343348510000026003456789012",
        defaultCardNumber = "4149 4990 9876 5432",
        brandColorHex = 0xFFB91C1C,
        secondaryColorHex = 0xFFFCA5A5,
        deepLinkScheme = "mybis://",           // офіційний deep-link схема додатку MyПУМБ
        webFallbackUrl = "https://www.pumb.ua/p2p", // Реальний платіжний P2P-розділ ПУМБ
        hasNativeP2pLink = false,
        packageName = "ua.pumb.pumbmobile"
    ),
    SENSE_BANK(
        id = "sense",
        displayName = "Sense Bank (Sense SuperApp)",
        shortName = "Sense Bank",
        defaultIban = "UA563003460000026005678901234",
        defaultCardNumber = "4242 4242 9988 7766",
        brandColorHex = 0xFF0284C7,
        secondaryColorHex = 0xFF38BDF8,
        deepLinkScheme = "sense://",           // deep-link схема додатку Sense SuperApp
        webFallbackUrl = "https://sensebank.ua/perevod-s-karty-na-kartu", // Реальний платіжний розділ переказів
        hasNativeP2pLink = false,
        packageName = "com.alfabank.mobile.android"
    ),
    ABANK(
        id = "abank",
        displayName = "А-Банк (ABank24)",
        shortName = "А-Банк",
        defaultIban = "UA783077700000026007890123456",
        defaultCardNumber = "4314 1414 3322 1100",
        brandColorHex = 0xFF16A34A,
        secondaryColorHex = 0xFFFACC15,
        deepLinkScheme = "abank24://",         // deep-link схема додатку ABank24
        webFallbackUrl = "https://a-bank.com.ua/transfers", // Реальний платіжний розділ переказів А-Банку
        hasNativeP2pLink = false,
        packageName = "ua.a_bank.mobile"
    )
}

data class MerchantProfile(
    val businessName: String = "",
    val taxNumber: String = "",
    val legalAddress: String = "",
    val defaultBank: UkrainianBank = UkrainianBank.PUMB,
    val iban: String = "UA573348510000026202124234254",
    val cardNumber: String = "",
    val phone: String = "",
    val email: String = "",
    val brandIcon: String = "⚡",
    val tagline: String = "Дякуємо за покупку! Слава Україні! 🇺🇦",
    val isPremium: Boolean = false,
    val monobankToken: String = "",
    val checkboxApiKey: String = "",
    val autoFiscalizeWithCheckbox: Boolean = false,
    val webhookGatewayUrl: String = "https://auratect-qr-pos-gateway.auratect.workers.dev",
    val customMerchantId: String = "",
    val pinHash: String = "",
    val biometricEnabled: Boolean = true,
    val voiceCashierEnabled: Boolean = false,
    val customerDisplayKeepScreenOn: Boolean = true,
    val serviceBalance: Double = 100.0
) {
    val merchantId: String
        get() = customMerchantId.ifBlank {
            if (taxNumber.isNotBlank()) "fop-$taxNumber" else "merchant-demo"
        }
    val checkboxToken: String get() = checkboxApiKey
    val isPinProtected: Boolean get() = pinHash.isNotBlank()
}

data class ProductItem(
    val id: String,
    val name: String,
    val price: Double,
    val category: String,
    val stock: Int = 99
)

data class CartItem(
    val product: ProductItem,
    val quantity: Int
)

enum class TransactionStatus {
    PENDING,
    PAID,
    CANCELLED
}

data class TransactionRecord(
    val id: String,
    val amount: Double,
    val feeAmount: Double,
    val netAmount: Double,
    val bank: UkrainianBank,
    val recipientName: String,
    val iban: String,
    val purpose: String,
    val itemsSummary: String,
    val timestamp: Long,
    val status: TransactionStatus = TransactionStatus.PAID,
    val tipsAmount: Double = 0.0
)
