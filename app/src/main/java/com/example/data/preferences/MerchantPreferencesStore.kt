package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.MerchantProfile
import com.example.model.UkrainianBank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.merchantDataStore: DataStore<Preferences> by preferencesDataStore(name = "merchant_preferences")

/**
 * Сховище для нечутливих налаштувань профілю мерчанта на базі DataStore<Preferences>.
 */
class MerchantPreferencesStore(private val context: Context) {

    private object PreferencesKeys {
        val HAS_SAVED_PROFILE = booleanPreferencesKey("has_saved_profile")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val TAX_NUMBER = stringPreferencesKey("tax_number")
        val LEGAL_ADDRESS = stringPreferencesKey("legal_address")
        val DEFAULT_BANK_NAME = stringPreferencesKey("default_bank_name")
        val IBAN = stringPreferencesKey("iban")
        val CARD_NUMBER = stringPreferencesKey("card_number")
        val PHONE = stringPreferencesKey("phone")
        val EMAIL = stringPreferencesKey("email")
        val BRAND_ICON = stringPreferencesKey("brand_icon")
        val TAGLINE = stringPreferencesKey("tagline")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val AUTO_FISCALIZE = booleanPreferencesKey("auto_fiscalize")
        val WEBHOOK_GATEWAY_URL = stringPreferencesKey("webhook_gateway_url")
        val CUSTOM_MERCHANT_ID = stringPreferencesKey("custom_merchant_id")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val VOICE_CASHIER_ENABLED = booleanPreferencesKey("voice_cashier_enabled")
        val CUSTOMER_DISPLAY_KEEP_SCREEN_ON = booleanPreferencesKey("customer_display_keep_screen_on")
        val SERVICE_BALANCE = doublePreferencesKey("service_balance")
    }

    /**
     * Потік спостереження за налаштуваннями мерчанта.
     * Повертає null, якщо профіль ще жодного разу не зберігався (перший запуск).
     */
    val merchantProfileFlow: Flow<MerchantProfile?> = context.merchantDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val hasSaved = prefs[PreferencesKeys.HAS_SAVED_PROFILE] ?: false
            if (!hasSaved) {
                null
            } else {
                val bankName = prefs[PreferencesKeys.DEFAULT_BANK_NAME]
                val bank = UkrainianBank.entries.firstOrNull { it.name == bankName } ?: UkrainianBank.PUMB

                MerchantProfile(
                    businessName = prefs[PreferencesKeys.BUSINESS_NAME] ?: "",
                    taxNumber = prefs[PreferencesKeys.TAX_NUMBER] ?: "",
                    legalAddress = prefs[PreferencesKeys.LEGAL_ADDRESS] ?: "",
                    defaultBank = bank,
                    iban = prefs[PreferencesKeys.IBAN] ?: bank.defaultIban,
                    cardNumber = prefs[PreferencesKeys.CARD_NUMBER] ?: "",
                    phone = prefs[PreferencesKeys.PHONE] ?: "",
                    email = prefs[PreferencesKeys.EMAIL] ?: "",
                    brandIcon = prefs[PreferencesKeys.BRAND_ICON]?.takeIf { it != "🏪" && it.isNotBlank() } ?: "⚡",
                    tagline = prefs[PreferencesKeys.TAGLINE] ?: "Дякуємо за покупку! Слава Україні! 🇺🇦",
                    isPremium = prefs[PreferencesKeys.IS_PREMIUM] ?: false,
                    autoFiscalizeWithCheckbox = prefs[PreferencesKeys.AUTO_FISCALIZE] ?: false,
                    webhookGatewayUrl = prefs[PreferencesKeys.WEBHOOK_GATEWAY_URL] ?: "https://auratect-qr-pos-gateway.auratect.workers.dev",
                    customMerchantId = prefs[PreferencesKeys.CUSTOM_MERCHANT_ID] ?: "",
                    pinHash = prefs[PreferencesKeys.PIN_HASH] ?: "",
                    biometricEnabled = prefs[PreferencesKeys.BIOMETRIC_ENABLED] ?: true,
                    voiceCashierEnabled = prefs[PreferencesKeys.VOICE_CASHIER_ENABLED] ?: false,
                    customerDisplayKeepScreenOn = prefs[PreferencesKeys.CUSTOMER_DISPLAY_KEEP_SCREEN_ON] ?: true,
                    serviceBalance = prefs[PreferencesKeys.SERVICE_BALANCE] ?: 100.0,
                    monobankToken = "", // Завантажується окремо з SecureTokenStore
                    checkboxApiKey = ""  // Завантажується окремо з SecureTokenStore
                )
            }
        }

    /**
     * Одноразове зчитування збереженого профілю при старті.
     */
    suspend fun getSavedProfile(): MerchantProfile? {
        return try {
            merchantProfileFlow.first()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Зберігає нечутливі параметри мерчанта в DataStore.
     */
    suspend fun saveProfile(profile: MerchantProfile) {
        context.merchantDataStore.edit { prefs ->
            prefs[PreferencesKeys.HAS_SAVED_PROFILE] = true
            prefs[PreferencesKeys.BUSINESS_NAME] = profile.businessName
            prefs[PreferencesKeys.TAX_NUMBER] = profile.taxNumber
            prefs[PreferencesKeys.LEGAL_ADDRESS] = profile.legalAddress
            prefs[PreferencesKeys.DEFAULT_BANK_NAME] = profile.defaultBank.name
            prefs[PreferencesKeys.IBAN] = profile.iban
            prefs[PreferencesKeys.CARD_NUMBER] = profile.cardNumber
            prefs[PreferencesKeys.PHONE] = profile.phone
            prefs[PreferencesKeys.EMAIL] = profile.email
            prefs[PreferencesKeys.BRAND_ICON] = profile.brandIcon
            prefs[PreferencesKeys.TAGLINE] = profile.tagline
            prefs[PreferencesKeys.IS_PREMIUM] = profile.isPremium
            prefs[PreferencesKeys.AUTO_FISCALIZE] = profile.autoFiscalizeWithCheckbox
            prefs[PreferencesKeys.WEBHOOK_GATEWAY_URL] = profile.webhookGatewayUrl
            prefs[PreferencesKeys.CUSTOM_MERCHANT_ID] = profile.customMerchantId
            prefs[PreferencesKeys.PIN_HASH] = profile.pinHash
            prefs[PreferencesKeys.BIOMETRIC_ENABLED] = profile.biometricEnabled
            prefs[PreferencesKeys.VOICE_CASHIER_ENABLED] = profile.voiceCashierEnabled
            prefs[PreferencesKeys.CUSTOMER_DISPLAY_KEEP_SCREEN_ON] = profile.customerDisplayKeepScreenOn
            prefs[PreferencesKeys.SERVICE_BALANCE] = profile.serviceBalance
        }
    }

    /**
     * Очищає збережені налаштування профілю з DataStore.
     */
    suspend fun clearProfile() {
        context.merchantDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
