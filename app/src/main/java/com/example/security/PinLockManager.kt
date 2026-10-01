package com.example.security

import java.security.MessageDigest

object PinLockManager {

    private const val SALT = "QR_POS_SECURITY_SALT_2026_UA"

    /**
     * Хешує 4-значний PIN-код за алгоритмом SHA-256 з персональною сіллю.
     */
    fun hashPin(pin: String): String {
        if (!isValidPinFormat(pin)) {
            throw IllegalArgumentException("PIN-код повинен складатися рівно з 4 цифр")
        }
        val input = "$SALT:$pin"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Перевіряє введений PIN-код проти збереженого хешу.
     */
    fun verifyPin(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || !isValidPinFormat(pin)) return false
        return hashPin(pin).equals(storedHash, ignoreCase = true)
    }

    /**
     * Валідує формат PIN-коду (рівно 4 десяткові цифри).
     */
    fun isValidPinFormat(pin: String): Boolean {
        return pin.length == 4 && pin.all { it.isDigit() }
    }
}
