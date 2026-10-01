package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "qr_pos_payments_channel"
    private const val CHANNEL_NAME = "QR POS Сповіщення про оплату"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Миттєві сповіщення про успішні надходження коштів на банківський рахунок"
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPaymentReceivedNotification(
        context: Context,
        amount: Double,
        bankName: String,
        transactionId: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formattedAmount = String.format(java.util.Locale.US, "%.2f", amount)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("✅ Зарахування коштів: +$formattedAmount ₴")
            .setContentText("Отримано успішний платіж через QR-код на рахунок $bankName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Кошти у розмірі $formattedAmount ₴ успішно надійшли на ваш рахунок в $bankName. Транзакція №${transactionId.takeLast(6).uppercase(java.util.Locale.ROOT)} підтверджена.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted or restricted
            e.printStackTrace()
        }
    }

    fun showOfflineSyncSuccessNotification(
        context: Context,
        syncedCount: Int
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("🧾 Офлайн-чеки успішно синхронізовано!")
            .setContentText("Фіскалізовано в ДПС (Checkbox): $syncedCount ${if (syncedCount == 1) "чек" else "чеків"}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Зв'язок відновлено. Всі накопичені чеки ($syncedCount шт.) успішно передано на фіскалізацію в податкову службу через Checkbox.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify((System.currentTimeMillis() + 1).toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
