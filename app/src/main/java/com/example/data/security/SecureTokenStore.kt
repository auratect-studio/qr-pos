package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Захищене сховище для чутливих ключів та токенів (Monobank API X-Token, Checkbox API Bearer Token тощо)
 * на основі EncryptedSharedPreferences з MasterKey через Android Keystore.
 */
class SecureTokenStore(context: Context) {

    private val prefs: SharedPreferences = initializeEncryptedPreferences(context)

    private fun initializeEncryptedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Помилка ініціалізації EncryptedSharedPreferences, спроба скидання ключа", e)
            try {
                // Видаляємо пошкоджений файл та ініціалізуємо заново
                context.deleteSharedPreferences(SECURE_PREFS_FILE)
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Критична помилка Android Keystore, використання резервного приватного SharedPreferences", fallbackEx)
                context.getSharedPreferences("${SECURE_PREFS_FILE}_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    /**
     * Записує чутливі токени у зашифроване сховище.
     */
    fun saveTokens(monobankToken: String, checkboxToken: String) {
        prefs.edit()
            .putString(KEY_MONOBANK_TOKEN, monobankToken.trim())
            .putString(KEY_CHECKBOX_TOKEN, checkboxToken.trim())
            .commit()
    }

    /**
     * Отримує збережений токен Monobank API.
     */
    fun getMonobankToken(): String = prefs.getString(KEY_MONOBANK_TOKEN, "") ?: ""

    /**
     * Отримує збережений токен ПРРО Checkbox.
     */
    fun getCheckboxToken(): String = prefs.getString(KEY_CHECKBOX_TOKEN, "") ?: ""

    /**
     * Повністю видаляє всі збережені токени із зашифрованого сховища.
     */
    fun clearTokens() {
        prefs.edit()
            .remove(KEY_MONOBANK_TOKEN)
            .remove(KEY_CHECKBOX_TOKEN)
            .commit()
    }

    companion object {
        private const val TAG = "SecureTokenStore"
        private const val SECURE_PREFS_FILE = "qrpos_secure_tokens"
        const val KEY_MONOBANK_TOKEN = "key_monobank_token"
        const val KEY_CHECKBOX_TOKEN = "key_checkbox_token"
    }
}
