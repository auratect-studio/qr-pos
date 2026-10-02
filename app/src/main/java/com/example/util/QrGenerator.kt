package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.UkrainianBank
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap
import java.util.Locale

object QrGenerator {

    /**
     * Generates a QR Bitmap for display in Compose UI.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 600,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.MARGIN, 1)
            }
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Constructs NBU (National Bank of Ukraine) standard EPC QR payload.
     * Complies with NBU standard for instant credit transfers.
     */
    fun buildNbuQrPayload(
        recipientName: String,
        iban: String,
        amount: Double,
        purpose: String
    ): String {
        val cleanIban = iban.replace("\\s".toRegex(), "").uppercase(Locale.ROOT)
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val effectiveRecipient = recipientName.trim().ifBlank { "ФОП Одержувач" }
        val effectivePurpose = purpose.trim().ifBlank { "Оплата замовлення" }

        return StringBuilder()
            .append("BCD\n")
            .append("002\n")
            .append("1\n")
            .append("SCT\n")
            .append("\n")
            .append(effectiveRecipient).append("\n")
            .append(cleanIban).append("\n")
            .append("UAH").append(formattedAmount).append("\n")
            .append("\n")
            .append("\n")
            .append(effectivePurpose).append("\n")
            .toString()
    }

    /**
     * Для QR-коду у режимі "Камера смартфона" (WEB_LINK):
     * Будує універсальне платіжне посилання (Universal Payment Link / Web Bridge).
     * При скануванні звичайною камерою будь-якого смартфона (iOS/Android):
     * 1. Відкриває платіжний міст з кнопками вибору Monobank, Privat24, ПУМБ, Sense, А-Банк.
     * 2. Дозволяє скопіювати IBAN безпосередньо на телефоні покупця в 1 дотик.
     * 3. Якщо вказана банківська картка (16 цифр), надає прямий перехід у Privat24.
     */
    fun buildBankPaymentUrl(
        bank: UkrainianBank,
        iban: String,
        amount: Double,
        purpose: String,
        recipientName: String = "",
        gatewayBaseUrl: String = "https://auratect-qr-pos-gateway.auratect.workers.dev",
        cardNumber: String = ""
    ): String {
        val cleanIban = iban.replace("\\s".toRegex(), "").uppercase(Locale.ROOT)
        val cleanCard = cardNumber.replace("\\s".toRegex(), "")
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val encodedPurpose = java.net.URLEncoder.encode(purpose.trim().ifBlank { "Оплата замовлення" }, "UTF-8")
        val encodedRecipient = java.net.URLEncoder.encode(recipientName.trim().ifBlank { "ФОП Одержувач" }, "UTF-8")

        // 1. Прямий переказ Privat24, якщо вказана 16-значна картка
        if (bank == UkrainianBank.PRIVATBANK && cleanCard.length == 16) {
            return "https://next.privat24.ua/money-transfer/card?recipient=$cleanCard&amount=$formattedAmount&purpose=$encodedPurpose"
        }

        // 2. Універсальний платіжний міст (Web Gateway)
        val cleanGateway = gatewayBaseUrl.trim().trimEnd('/')
        if (cleanGateway.isNotBlank()) {
            val pumbParam = if (bank == UkrainianBank.PUMB) "&pumb_url=" + java.net.URLEncoder.encode("https://mobile-app.pumb.ua/1YAsa", "UTF-8") else ""
            return "$cleanGateway/pay?to=$encodedRecipient&iban=$cleanIban&amount=$formattedAmount&purpose=$encodedPurpose&bank=${bank.id}&card=$cleanCard$pumbParam"
        }

        // 3. Прямі платіжні посилання (fallback)
        return when (bank) {
            UkrainianBank.MONOBANK ->
                "https://send.monobank.ua/?amount=$formattedAmount&destination=$cleanIban&description=$encodedPurpose"
            UkrainianBank.PRIVATBANK ->
                "https://next.privat24.ua/money-transfer/card?amount=$formattedAmount&purpose=$encodedPurpose"
            UkrainianBank.PUMB ->
                "https://mobile-app.pumb.ua/1YAsa"
            UkrainianBank.SENSE_BANK ->
                "https://sensebank.ua/perevod-s-karty-na-kartu"
            UkrainianBank.ABANK ->
                "https://a-bank.com.ua/transfers"
        }
    }

    /**
     * Будує список Intent-ів для відкриття банківського додатку:
     * 1. Намагається відкрити через URI-схему (deep link) — відкриє додаток якщо встановлений.
     * 2. Fallback — відкриває офіційний сайт банку у браузері.
     * Повертає пару (deepLinkUri, webFallbackUri).
     */
    fun buildBankOpenIntents(bank: UkrainianBank): Pair<String, String> {
        return Pair(bank.deepLinkScheme, bank.webFallbackUrl)
    }
}
