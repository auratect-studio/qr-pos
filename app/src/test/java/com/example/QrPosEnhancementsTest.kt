package com.example

import com.example.data.db.ProductEntity
import com.example.data.db.TransactionEntity
import com.example.model.ProductItem
import com.example.model.QrPaymentMode
import com.example.model.TransactionRecord
import com.example.model.TransactionStatus
import com.example.model.UkrainianBank
import com.example.util.EscPosPrinterHelper
import com.example.util.QrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPosEnhancementsTest {

    @Test
    fun testQrPaymentModeEnumProperties() {
        assertEquals("Камера", QrPaymentMode.WEB_LINK.title)
        assertEquals("НБУ QR", QrPaymentMode.NBU_STANDARD.title)
        assertTrue(QrPaymentMode.WEB_LINK.instruction.contains("камерою"))
        assertTrue(QrPaymentMode.NBU_STANDARD.instruction.contains("Monobank"))
    }

    @Test
    fun testQrGeneratorNbuAndWebPayloads() {
        val nbuPayload = QrGenerator.buildNbuQrPayload(
            recipientName = "ФОП Шевченко",
            iban = "UA213220010000026007234567890",
            amount = 150.0,
            purpose = "Оплата за послуги"
        )
        assertTrue(nbuPayload.startsWith("BCD\n002\n1\nSCT"))
        assertTrue(nbuPayload.contains("UA213220010000026007234567890"))
        assertTrue(nbuPayload.contains("UAH150.00"))

        val bankUrl = QrGenerator.buildBankPaymentUrl(
            bank = UkrainianBank.MONOBANK,
            iban = "UA213220010000026007234567890",
            amount = 150.0,
            purpose = "Оплата за послуги"
        )
        assertTrue(bankUrl.contains("/pay"))
        assertTrue(bankUrl.contains("150.00"))
        assertTrue(bankUrl.contains("UA213220010000026007234567890"))
    }

    @Test
    fun testEscPosReceiptBytesGeneration() {
        val tx = TransactionRecord(
            id = "tx-test-99",
            amount = 250.0,
            feeAmount = 1.25,
            netAmount = 248.75,
            bank = UkrainianBank.MONOBANK,
            recipientName = "ФОП Петренко",
            iban = "UA112233445566778899001122334",
            purpose = "Тестовий платіж",
            itemsSummary = "2x Капучино",
            timestamp = System.currentTimeMillis(),
            status = TransactionStatus.PAID
        )

        val bytes = EscPosPrinterHelper.buildEscPosReceiptBytes(transaction = tx, merchant = null)
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        // Check for init command bytes 0x1B 0x40
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun testProductEntityMapping() {
        val model = ProductItem("p-test", "Американо", 50.0, "Кава", 20)
        val entity = ProductEntity.fromModel(model)
        assertEquals("p-test", entity.id)
        assertEquals("Американо", entity.name)
        assertEquals(50.0, entity.price, 0.001)

        val convertedBack = entity.toModel()
        assertEquals(model, convertedBack)
    }

    @Test
    fun testTransactionEntityMapping() {
        val tx = TransactionRecord(
            id = "tx-db-01",
            amount = 99.0,
            feeAmount = 0.0,
            netAmount = 99.0,
            bank = UkrainianBank.PRIVATBANK,
            recipientName = "ФОП Іванов",
            iban = "UA123456789",
            purpose = "Круасан",
            itemsSummary = "1x Круасан",
            timestamp = 1700000000000L,
            status = TransactionStatus.PAID
        )
        val entity = TransactionEntity.fromModel(tx)
        assertEquals("privat", entity.bankId)
        assertEquals("PAID", entity.status)

        val convertedBack = entity.toModel()
        assertEquals(tx.id, convertedBack.id)
        assertEquals(tx.bank, convertedBack.bank)
        assertEquals(tx.amount, convertedBack.amount, 0.001)
    }

    @Test
    fun testPlatformBillingConfigRatesAndCoveredTurnover() {
        assertEquals(0.008, com.example.config.PlatformBillingConfig.BASE_COMMISSION_RATE, 0.0001)
        assertEquals(0.005, com.example.config.PlatformBillingConfig.TIPS_COMMISSION_RATE, 0.0001)
        assertEquals(100.0, com.example.config.PlatformBillingConfig.WELCOME_BONUS_BALANCE, 0.001)

        val covered = com.example.config.PlatformBillingConfig.calculateCoveredTurnover(100.0)
        assertEquals(12500.0, covered, 0.01)
    }

    @Test
    fun testCommissionCalculationAndBalanceDeduction() {
        val baseAmount = 1000.0
        val tipAmount = 100.0
        val baseFee = baseAmount * com.example.config.PlatformBillingConfig.BASE_COMMISSION_RATE // 8.0 ₴ (0.8%)
        val tipFee = tipAmount * com.example.config.PlatformBillingConfig.TIPS_COMMISSION_RATE // 0.5 ₴ (0.5%)
        val totalFee = baseFee + tipFee

        assertEquals(8.0, baseFee, 0.001)
        assertEquals(0.5, tipFee, 0.001)
        assertEquals(8.5, totalFee, 0.001)

        var balance = com.example.config.PlatformBillingConfig.WELCOME_BONUS_BALANCE
        balance -= totalFee
        assertEquals(91.5, balance, 0.001)

        // Top-up simulation
        balance += 250.0
        assertEquals(341.5, balance, 0.001)
    }
}