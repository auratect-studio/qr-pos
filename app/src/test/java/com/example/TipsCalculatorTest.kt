package com.example

import com.example.ui.components.TipOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TipsCalculatorTest {

    private fun calculateTip(baseAmount: Double, option: TipOption): Double {
        val pct = option.percentage ?: return 0.0
        return Math.round(baseAmount * (pct / 100.0) * 100.0) / 100.0
    }

    private fun calculateCommission(
        baseAmount: Double,
        tipAmount: Double,
        isPremium: Boolean,
        baseRate: Double = 0.005,      // 0.5% Free base
        tipRate: Double = 0.0025       // 0.25% Free anti-abuse tip discount (50% off)
    ): Double {
        if (isPremium) return 0.0
        val baseFee = baseAmount * baseRate
        val tipFee = tipAmount * tipRate
        return Math.round((baseFee + tipFee) * 100.0) / 100.0
    }

    @Test
    fun testPresetTipCalculations() {
        val base = 200.0

        val tipNone = calculateTip(base, TipOption.NONE)
        val tip5 = calculateTip(base, TipOption.FIVE)
        val tip10 = calculateTip(base, TipOption.TEN)
        val tip15 = calculateTip(base, TipOption.FIFTEEN)

        assertEquals(0.0, tipNone, 0.001)
        assertEquals(10.0, tip5, 0.001)   // 5% of 200 = 10 ₴
        assertEquals(20.0, tip10, 0.001)  // 10% of 200 = 20 ₴
        assertEquals(30.0, tip15, 0.001)  // 15% of 200 = 30 ₴
    }

    @Test
    fun testTipRoundingWithOddAmounts() {
        val base = 137.50

        val tip5 = calculateTip(base, TipOption.FIVE)   // 137.5 * 0.05 = 6.875 -> 6.88
        val tip10 = calculateTip(base, TipOption.TEN)  // 137.5 * 0.10 = 13.75
        val tip15 = calculateTip(base, TipOption.FIFTEEN) // 137.5 * 0.15 = 20.625 -> 20.63

        assertEquals(6.88, tip5, 0.001)
        assertEquals(13.75, tip10, 0.001)
        assertEquals(20.63, tip15, 0.001)
    }

    @Test
    fun testAntiAbuseCommissionModelFreeVsPro() {
        val base = 500.0
        val tip = 50.0

        // In Free plan:
        // Base commission (0.5%): 500 * 0.005 = 2.50 ₴
        // Tip commission (0.25% anti-abuse): 50 * 0.0025 = 0.125 -> 0.13 ₴
        // Total: 2.63 ₴
        val freeFee = calculateCommission(base, tip, isPremium = false, baseRate = 0.005, tipRate = 0.0025)
        assertEquals(2.63, freeFee, 0.01)

        // In Pro plan:
        // 0% on everything
        val proFee = calculateCommission(base, tip, isPremium = true)
        assertEquals(0.0, proFee, 0.0001)
    }

    @Test
    fun testAntiAbusePreventsZeroFeeManipulation() {
        // Attempt to abuse by declaring 1 ₴ base and 499 ₴ tips
        val base = 1.0
        val tip = 499.0

        val freeFee = calculateCommission(base, tip, isPremium = false, baseRate = 0.005, tipRate = 0.0025)

        // With anti-abuse 0.25%, tips still incur 499 * 0.0025 = 1.25 ₴, preventing free abuse
        assertTrue(freeFee > 1.20)
    }

    @Test
    fun testTotalPayableAmount() {
        val base = 350.0
        val tip = 35.0
        val total = base + tip
        assertEquals(385.0, total, 0.001)
    }

    @Test
    fun testCustomTipParsing() {
        val inputComma = "25,50"
        val parsedComma = inputComma.replace(',', '.').toDoubleOrNull() ?: 0.0
        assertEquals(25.50, parsedComma, 0.001)

        val inputDot = "40.00"
        val parsedDot = inputDot.replace(',', '.').toDoubleOrNull() ?: 0.0
        assertEquals(40.00, parsedDot, 0.001)

        val inputInvalid = "abc"
        val parsedInvalid = inputInvalid.replace(',', '.').toDoubleOrNull() ?: 0.0
        assertEquals(0.0, parsedInvalid, 0.001)
    }
}
