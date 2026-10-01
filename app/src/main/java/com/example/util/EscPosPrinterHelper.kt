package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import com.example.model.MerchantProfile
import com.example.model.TransactionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object EscPosPrinterHelper {

    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS Command Constants
    val CMD_INIT = byteArrayOf(0x1B, 0x40)
    val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    val CMD_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    val CMD_DOUBLE_SIZE = byteArrayOf(0x1B, 0x21, 0x30)
    val CMD_NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00)
    val CMD_SELECT_CP866 = byteArrayOf(0x1B, 0x74, 17) // CP866 Cyrillic table
    val CMD_CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

    /**
     * Повертає список спарених Bluetooth-пристроїв із безпечною перевіркою дозволів.
     */
    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<BluetoothDevice> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) return emptyList()
            }
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Формує байтовий потік ESC/POS для друку чека.
     */
    fun buildEscPosReceiptBytes(
        transaction: TransactionRecord,
        merchant: MerchantProfile?
    ): ByteArray {
        val out = ByteArrayOutputStream()

        fun writeBytes(bytes: ByteArray) {
            out.write(bytes)
        }

        fun writeText(text: String, bold: Boolean = false, center: Boolean = false, doubleSize: Boolean = false) {
            if (center) writeBytes(CMD_ALIGN_CENTER) else writeBytes(CMD_ALIGN_LEFT)
            if (bold) writeBytes(CMD_BOLD_ON) else writeBytes(CMD_BOLD_OFF)
            if (doubleSize) writeBytes(CMD_DOUBLE_SIZE) else writeBytes(CMD_NORMAL_SIZE)

            // Спроба кодування Windows-1251 або CP866 з резервом на UTF-8
            val encoded = try {
                text.toByteArray(Charset.forName("windows-1251"))
            } catch (_: Exception) {
                try {
                    text.toByteArray(Charset.forName("CP866"))
                } catch (_: Exception) {
                    text.toByteArray(Charsets.UTF_8)
                }
            }
            writeBytes(encoded)
            writeBytes("\n".toByteArray())
        }

        // 1. Ініціалізація та вибір кодової сторінки
        writeBytes(CMD_INIT)
        writeBytes(CMD_SELECT_CP866)

        // 2. Шапка чека
        val brand = merchant?.brandIcon ?: "⚡"
        val orgName = merchant?.businessName.takeIf { !it.isNullOrBlank() } ?: transaction.recipientName
        writeText("================================", center = true)
        writeText("  ЕЛЕКТРОННИЙ ЧЕК  ", bold = true, center = true)
        writeText("QR-POS TERMINAL", bold = true, center = true)
        writeText("================================", center = true)

        writeText("Організація: $orgName")
        val tax = merchant?.taxNumber.takeIf { !it.isNullOrBlank() } ?: "3123456789"
        writeText("ІПН / ЄДРПОУ: $tax")
        merchant?.legalAddress?.takeIf { it.isNotBlank() }?.let { writeText("Адреса: $it") }
        merchant?.phone?.takeIf { it.isNotBlank() }?.let { writeText("Тел: $it") }

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.forLanguageTag("uk-UA"))
        writeText("Дата: ${sdf.format(Date(transaction.timestamp))}")
        writeText("Чек №: ${transaction.id.takeLast(8)}")
        writeText("Банк: ${transaction.bank.displayName}")
        writeText("IBAN: ${transaction.iban}")
        writeText("--------------------------------", center = true)

        // 3. Позиції / Призначення
        writeText("ПРИЗНАЧЕННЯ / ТОВАРИ:")
        writeText(transaction.itemsSummary.ifBlank { transaction.purpose })
        writeText("--------------------------------", center = true)

        // 4. Сума та комісія
        val sumStr = String.format(Locale.US, "%.2f", transaction.amount)
        writeText("СУМА ДО СПЛАТИ: $sumStr UAH", bold = true, doubleSize = false)
        if (transaction.feeAmount > 0) {
            val feeStr = String.format(Locale.US, "%.2f", transaction.feeAmount)
            writeText("Комісія сервісу: $feeStr UAH")
        } else {
            writeText("Комісія (Преміум Pro): 0.00 UAH (0%)")
        }
        val netStr = String.format(Locale.US, "%.2f", transaction.netAmount)
        writeText("Зараховано продавцю: $netStr UAH", bold = true)

        writeText("Статус: ОПЛАЧЕНО (БЕЗГОТІВКОВО)")
        writeText("Спосіб: QR-код (${transaction.bank.displayName})")
        writeText("================================", center = true)

        // 5. Фінал
        val tagline = merchant?.tagline.takeIf { !it.isNullOrBlank() } ?: "Дякуємо за покупку!"
        writeText(tagline, bold = true, center = true)
        writeText("Сформовано через QR-POS", center = true)

        // Подача паперу на 4 рядки вперед та обрізка
        writeBytes("\n\n\n\n".toByteArray())
        writeBytes(CMD_CUT_PAPER)

        return out.toByteArray()
    }

    /**
     * Відправляє підготовлені байти на Bluetooth термопринтер (безпечна обробка помилок).
     */
    @SuppressLint("MissingPermission")
    suspend fun printToBluetoothDevice(
        device: BluetoothDevice,
        data: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val stream = socket.outputStream
            stream.write(data)
            stream.flush()
            Result.success(Unit)
        } catch (e: Throwable) {
            val devName = try { device.name ?: device.address } catch (_: Throwable) { device.address }
            Result.failure(Exception("Помилка зв'язку з принтером $devName: ${e.localizedMessage ?: "немає зв'язку"}"))
        } finally {
            try {
                socket?.close()
            } catch (_: Throwable) {}
        }
    }
}