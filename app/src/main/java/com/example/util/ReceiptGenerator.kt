package com.example.util

import com.example.model.CartItem
import com.example.model.MerchantProfile
import com.example.model.TransactionRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptGenerator {

    fun generateReceiptText(
        transaction: TransactionRecord,
        cartItems: List<CartItem> = emptyList(),
        taxNumber: String = "3123456789",
        merchant: MerchantProfile? = null
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.forLanguageTag("uk-UA"))
        val dateString = dateFormat.format(Date(transaction.timestamp))
        val sb = StringBuilder()

        val brand = merchant?.brandIcon ?: "⚡"
        val name = merchant?.businessName ?: transaction.recipientName
        val tax = merchant?.taxNumber ?: taxNumber
        val address = merchant?.legalAddress
        val phone = merchant?.phone
        val card = merchant?.cardNumber
        val tagline = merchant?.tagline

        sb.append("================================\n")
        sb.append("       $brand  ЕЛЕКТРОННИЙ ЧЕК  $brand\n")
        sb.append("        QR POS TERMINAL         \n")
        sb.append("================================\n")
        sb.append("Організація: $name\n")
        sb.append("ІПН/ЄДРПОУ: $tax\n")
        if (!address.isNullOrBlank()) {
            sb.append("Адреса: $address\n")
        }
        if (!phone.isNullOrBlank()) {
            sb.append("Тел: $phone\n")
        }
        sb.append("Дата і час: $dateString\n")
        sb.append("Чек №: ${transaction.id.takeLast(8).uppercase(Locale.ROOT)}\n")
        sb.append("Банк еквайр: ${transaction.bank.shortName}\n")
        sb.append("Рахунок (IBAN): ${transaction.iban}\n")
        if (!card.isNullOrBlank()) {
            val masked = if (card.length >= 8) card.take(4) + " •••• •••• " + card.takeLast(4) else card
            sb.append("Картка зарахування: $masked\n")
        }
        sb.append("--------------------------------\n")

        if (cartItems.isNotEmpty()) {
            sb.append("ТОВАРИ / ПОСЛУГИ:\n")
            cartItems.forEachIndexed { index, item ->
                val total = item.product.price * item.quantity
                sb.append("${index + 1}. ${item.product.name}\n")
                sb.append("   ${item.quantity} шт. х ${String.format(Locale.US, "%.2f", item.product.price)} ₴ = ${String.format(Locale.US, "%.2f", total)} ₴\n")
            }
            sb.append("--------------------------------\n")
        } else {
            sb.append("Призначення: ${transaction.purpose}\n")
            sb.append("--------------------------------\n")
        }

        sb.append("СУМА ДО СПЛАТИ: ${String.format(Locale.US, "%.2f", transaction.amount)} ₴\n")
        if (transaction.feeAmount > 0) {
            sb.append("Комісія сервісу (0.5%): ${String.format(Locale.US, "%.2f", transaction.feeAmount)} ₴\n")
            sb.append("До зарахування продавцю: ${String.format(Locale.US, "%.2f", transaction.netAmount)} ₴\n")
        } else {
            sb.append("Комісія (Преміум Pro): 0.00 ₴ (0%)\n")
            sb.append("До зарахування продавцю: ${String.format(Locale.US, "%.2f", transaction.netAmount)} ₴\n")
        }
        sb.append("Статус: ОПЛАЧЕНО (БЕЗГОТІВКОВО)\n")
        sb.append("Спосіб оплати: QR-код (${transaction.bank.shortName})\n")
        sb.append("================================\n")
        if (!tagline.isNullOrBlank()) {
            sb.append("$tagline\n")
        } else {
            sb.append("Дякуємо за покупку!\n")
        }
        sb.append("Сформовано через QR POS Terminal\n")

        return sb.toString()
    }
}
