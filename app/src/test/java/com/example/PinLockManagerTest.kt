package com.example

import com.example.security.PinLockManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinLockManagerTest {

    @Test
    fun testValidPinFormat() {
        assertTrue(PinLockManager.isValidPinFormat("1234"))
        assertTrue(PinLockManager.isValidPinFormat("0000"))
        assertTrue(PinLockManager.isValidPinFormat("9876"))
        assertTrue(PinLockManager.isValidPinFormat("0123"))

        assertFalse(PinLockManager.isValidPinFormat("123"))      // 3 digits
        assertFalse(PinLockManager.isValidPinFormat("12345"))    // 5 digits
        assertFalse(PinLockManager.isValidPinFormat("12a4"))     // letter
        assertFalse(PinLockManager.isValidPinFormat(""))         // empty
        assertFalse(PinLockManager.isValidPinFormat("    "))     // spaces
        assertFalse(PinLockManager.isValidPinFormat("12-4"))     // symbol
    }

    @Test
    fun testHashPinConsistency() {
        val hash1 = PinLockManager.hashPin("2580")
        val hash2 = PinLockManager.hashPin("2580")
        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 hex is 64 characters
    }

    @Test
    fun testDistinctPinsProduceDistinctHashes() {
        val hashA = PinLockManager.hashPin("1111")
        val hashB = PinLockManager.hashPin("2222")
        assertNotEquals(hashA, hashB)
    }

    @Test
    fun testVerifyPinSuccess() {
        val pin = "4321"
        val hash = PinLockManager.hashPin(pin)

        assertTrue(PinLockManager.verifyPin("4321", hash))
    }

    @Test
    fun testVerifyPinFailure() {
        val hash = PinLockManager.hashPin("1234")

        assertFalse(PinLockManager.verifyPin("9999", hash))
        assertFalse(PinLockManager.verifyPin("1235", hash))
        assertFalse(PinLockManager.verifyPin("123", hash))
        assertFalse(PinLockManager.verifyPin("", hash))
        assertFalse(PinLockManager.verifyPin("1234", ""))
        assertFalse(PinLockManager.verifyPin("1234", "invalid_hash"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testHashPinThrowsOnInvalidLength() {
        PinLockManager.hashPin("12")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testHashPinThrowsOnLetters() {
        PinLockManager.hashPin("abcd")
    }
}
