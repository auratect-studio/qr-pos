package com.example.network.webhook

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Нормалізована подія зарахування коштів від платіжного шлюзу (Webhook Gateway).
 * Працює через Server-Sent Events (SSE) в реальному часі без перевантаження Firebase.
 */
@JsonClass(generateAdapter = true)
data class BankWebhookEvent(
    @Json(name = "event") val event: String = "PAYMENT_RECEIVED",
    @Json(name = "merchantId") val merchantId: String = "",
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "currency") val currency: String = "UAH",
    @Json(name = "bank") val bank: String = "MONOBANK",
    @Json(name = "transactionId") val transactionId: String = "",
    @Json(name = "comment") val comment: String = "",
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
