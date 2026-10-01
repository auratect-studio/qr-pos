package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceCashierHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var pendingAnnouncement: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ukLocale = Locale("uk", "UA")
            val result = tts?.setLanguage(ukLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceCashier", "Ukrainian TTS data missing or not supported, falling back to default")
                tts?.language = Locale.getDefault()
            }
            tts?.setSpeechRate(1.05f)
            tts?.setPitch(1.0f)
            isInitialized = true

            // Відтворюємо відкладене оголошення, якщо платіж надійшов до закінчення ініціалізації TTS
            pendingAnnouncement?.let { phrase ->
                tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "PAYMENT_ANNOUNCEMENT_${System.currentTimeMillis()}")
                pendingAnnouncement = null
            }
        } else {
            Log.e("VoiceCashier", "Failed to initialize TextToSpeech: status=$status")
        }
    }

    /**
     * Озвучує суму оплати українською мовою з правильними відмінками (гривня, гривні, гривень).
     */
    fun announcePayment(amount: Double) {
        val phrase = formatUkrainianCurrencySpeech(amount)
        if (!isInitialized) {
            pendingAnnouncement = phrase
            return
        }
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "PAYMENT_ANNOUNCEMENT_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        fun formatUkrainianCurrencySpeech(amount: Double): String {
            val wholePart = amount.toInt()
            val kopecks = Math.round((amount - wholePart) * 100).toInt()

            val hryvniaWord = getHryvniaDeclension(wholePart)
            val base = "Оплачено $wholePart $hryvniaWord"

            return if (kopecks > 0) {
                val kopeckWord = getKopeckDeclension(kopecks)
                "$base $kopecks $kopeckWord"
            } else {
                base
            }
        }

        private fun getHryvniaDeclension(n: Int): String {
            val mod100 = n % 100
            val mod10 = n % 10
            return when {
                mod100 in 11..19 -> "гривень"
                mod10 == 1 -> "гривня"
                mod10 in 2..4 -> "гривні"
                else -> "гривень"
            }
        }

        private fun getKopeckDeclension(n: Int): String {
            val mod100 = n % 100
            val mod10 = n % 10
            return when {
                mod100 in 11..19 -> "копійок"
                mod10 == 1 -> "копійка"
                mod10 in 2..4 -> "копійки"
                else -> "копійок"
            }
        }
    }
}
