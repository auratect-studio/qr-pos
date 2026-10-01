package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Допоміжний сервіс для миттєвого відтворення звуку успішної касової оплати (дзвіночок "Дзинь-дзинь!")
 * та тактильної вібрації безпосередньо через звуковий процесор пристрою.
 */
object PaymentSoundHelper {

    private const val SAMPLE_RATE = 44100
    private const val TAG = "PaymentSoundHelper"
    @Volatile
    private var lastPlayTimestamp: Long = 0L

    /**
     * Відтворює касовий акорд успішної оплати та вібрацію.
     */
    fun playPaymentSuccessSound(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastPlayTimestamp < 600) {
            // Захист від подвійного паралельного виклику або накладання звуків
            return
        }
        lastPlayTimestamp = now

        // 1. Тактильний відгук (дві приємні касові вібрації)
        triggerHapticSuccess(context)

        // 2. Безпосереднє відтворення касового дзвону у фоновому потоці через PCM AudioTrack
        CoroutineScope(Dispatchers.Default).launch {
            try {
                playChimeSound()
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack failed, falling back to system notification sound", e)
                playSystemRingtone(context)
            }
        }
    }

    private fun triggerHapticSuccess(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 60, 50, 90)
                    val amplitudes = intArrayOf(0, 190, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 60, 50, 90), -1)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic feedback unavailable: ${e.message}")
        }
    }

    private fun playChimeSound() {
        val durationMs = 420
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(numSamples)

        // Чистий мажорний інтервал POS-терміналу: Нота 1 (C6 ~ 1046.5 Hz), Нота 2 (E6 ~ 1318.5 Hz), гармоніка (C7 ~ 2093 Hz)
        val tone1Freq = 1046.5
        val tone2Freq = 1318.5
        val tone3Freq = 2093.0

        val transitionSample = (SAMPLE_RATE * 90) / 1000 // перша нотка 90 мс, далі акорд

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            // Експоненційне природне згасання дзвіночка
            val envelope = (1.0 - progress) * (1.0 - progress * 0.7)

            val sampleVal: Double = if (i < transitionSample) {
                // Нота 1: вступний дзвінок
                sin(2.0 * PI * tone1Freq * t) * 0.80 * envelope
            } else {
                // Нота 2 + 3: підтверджуючий дзвінкий мажорний інтервал
                (sin(2.0 * PI * tone2Freq * t) * 0.65 + sin(2.0 * PI * tone3Freq * t) * 0.35) * envelope
            }

            buffer[i] = (sampleVal * Short.MAX_VALUE * 0.90)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()

        // Очікуємо закінчення звучання перед вивільненням ресурсів
        Thread.sleep(durationMs.toLong() + 60)
        try {
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {}
    }

    private fun playSystemRingtone(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone.play()
        } catch (_: Exception) {}
    }
}
