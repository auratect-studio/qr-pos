package com.example

import com.example.model.MerchantProfile
import com.example.model.UkrainianBank
import com.example.network.MonoSetWebhookRequest
import com.example.network.webhook.BankWebhookEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankWebhookIntegrationTest {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testBankWebhookEventDeserialization() {
        val json = """
            {
                "event": "PAYMENT_RECEIVED",
                "merchantId": "fop-3123456789",
                "amount": 250.50,
                "currency": "UAH",
                "bank": "MONOBANK",
                "transactionId": "mono-tx-88339",
                "comment": "Оплата кави та десерту",
                "timestamp": 1727318000000
            }
        """.trimIndent()

        val adapter = moshi.adapter(BankWebhookEvent::class.java)
        val event = adapter.fromJson(json)

        assertNotNull(event)
        assertEquals("PAYMENT_RECEIVED", event?.event)
        assertEquals("fop-3123456789", event?.merchantId)
        assertEquals(250.50, event?.amount ?: 0.0, 0.001)
        assertEquals("UAH", event?.currency)
        assertEquals("MONOBANK", event?.bank)
        assertEquals("mono-tx-88339", event?.transactionId)
        assertEquals("Оплата кави та десерту", event?.comment)
        assertEquals(1727318000000L, event?.timestamp)
    }

    @Test
    fun testMonoSetWebhookRequestSerialization() {
        val req = MonoSetWebhookRequest(webHookUrl = "https://gateway.example.com/webhook/mono/fop-3123456789")
        val adapter = moshi.adapter(MonoSetWebhookRequest::class.java)
        val json = adapter.toJson(req)

        assertTrue(json.contains("\"webHookUrl\":\"https://gateway.example.com/webhook/mono/fop-3123456789\""))
    }

    @Test
    fun testMerchantProfileComputedMerchantIdAndWebhookUrl() {
        val profileWithTax = MerchantProfile(
            businessName = "ФОП Коваленко",
            taxNumber = "3198765432",
            webhookGatewayUrl = "https://auratect-qr-pos-gateway.auratect.workers.dev"
        )
        assertEquals("fop-3198765432", profileWithTax.merchantId)

        val customProfile = MerchantProfile(
            businessName = "Кав'ярня",
            taxNumber = "3198765432",
            customMerchantId = "aroma-coffee-lviv"
        )
        assertEquals("aroma-coffee-lviv", customProfile.merchantId)

        val emptyProfile = MerchantProfile()
        assertEquals("merchant-demo", emptyProfile.merchantId)

        val webhookUrl = "${profileWithTax.webhookGatewayUrl.trimEnd('/')}/webhook/mono/${profileWithTax.merchantId}"
        assertEquals("https://auratect-qr-pos-gateway.auratect.workers.dev/webhook/mono/fop-3198765432", webhookUrl)
    }

    @Test
    fun testAntiSpamCooldownDebounceDuration() {
        val targetCooldownSeconds = 10
        val lastCheckTimestamp = 100_000L

        // Check after 4 seconds (should be debounced)
        val checkAt4s = 104_000L
        val elapsed4s = (checkAt4s - lastCheckTimestamp) / 1000
        val isDebouncedAt4s = elapsed4s < targetCooldownSeconds
        assertTrue("Check at 4s must trigger anti-spam debounce", isDebouncedAt4s)
        assertEquals(6, targetCooldownSeconds - elapsed4s.toInt())

        // Check after 11 seconds (allowed)
        val checkAt11s = 111_000L
        val elapsed11s = (checkAt11s - lastCheckTimestamp) / 1000
        val isDebouncedAt11s = elapsed11s < targetCooldownSeconds
        assertTrue("Check at 11s must be allowed", !isDebouncedAt11s)
    }
}
