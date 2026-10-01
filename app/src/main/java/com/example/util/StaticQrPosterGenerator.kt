package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.MerchantProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StaticQrPosterGenerator {

    /**
     * Генерує стильний друкований PDF-постер формата A4 зі статичним IBAN QR-кодом мерчанта.
     */
    fun generatePosterPdf(context: Context, merchant: MerchantProfile): File {
        val pdfDocument = PdfDocument()

        // Стандартний формат A4: 595 x 842 пунктів (72 dpi)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Білий фон
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // 2. Декоративна рамка
        paint.color = Color.parseColor("#059669")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        val frameRect = RectF(24f, 24f, (pageWidth - 24).toFloat(), (pageHeight - 24).toFloat())
        canvas.drawRoundRect(frameRect, 24f, 24f, paint)

        // 3. Шапка (Emerald Header)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#059669")
        val headerRect = RectF(24f, 24f, (pageWidth - 24).toFloat(), 130f)
        canvas.drawRoundRect(headerRect, 24f, 24f, paint)
        canvas.drawRect(24f, 80f, (pageWidth - 24).toFloat(), 130f, paint)

        // Текст шапки
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("⚡ QR POS TERMINAL", pageWidth / 2f, 75f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("ШВИДКА ТА БЕЗПЕЧНА ОПЛАТА БЕЗ КОМІСІЇ ДЛЯ КЛІЄНТА", pageWidth / 2f, 105f, paint)

        // 4. Блок даних торгової точки
        paint.color = Color.parseColor("#0F172A")
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 24f
        paint.isFakeBoldText = true
        val businessTitle = merchant.businessName.ifBlank { "Мій Бізнес" }
        canvas.drawText(businessTitle, pageWidth / 2f, 175f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#475569")
        val taxText = if (merchant.taxNumber.isNotBlank()) "ІПН/ЄДРПОУ: ${merchant.taxNumber} • " else ""
        canvas.drawText("${taxText}Банк: ${merchant.defaultBank.displayName}", pageWidth / 2f, 200f, paint)

        // 5. Векторний QR-код стандарту НБУ
        val qrPayload = QrGenerator.buildNbuQrPayload(
            recipientName = merchant.businessName.ifBlank { "Торгова точка" },
            iban = merchant.iban.ifBlank { merchant.defaultBank.defaultIban },
            amount = 0.0,
            purpose = "Оплата за товари та послуги"
        )

        val qrBitmap = QrGenerator.generateQrBitmap(qrPayload, 320)
        val qrLeft = (pageWidth - 320) / 2f
        val qrTop = 230f

        // Біла підкладка з тінню/рамкою навколо QR
        paint.color = Color.parseColor("#F1F5F9")
        paint.style = Paint.Style.FILL
        val qrBgRect = RectF(qrLeft - 16f, qrTop - 16f, qrLeft + 320f + 16f, qrTop + 320f + 16f)
        canvas.drawRoundRect(qrBgRect, 20f, 20f, paint)

        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(qrBgRect, 20f, 20f, paint)

        // Малювання QR
        paint.style = Paint.Style.FILL
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, paint)
        }

        // 6. Реквізити IBAN під кодом
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 13f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Рахунок (IBAN):", pageWidth / 2f, 595f, paint)

        paint.textSize = 13f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#059669")
        canvas.drawText(merchant.iban.ifBlank { merchant.defaultBank.defaultIban }, pageWidth / 2f, 615f, paint)

        // 7. Покрокова інструкція для покупця
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText("Як оплатити за 5 секунд:", pageWidth / 2f, 655f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#334155")
        canvas.drawText("1. Відкрийте камеру або банківський додаток (monobank, Приват24, ПУМБ тощо)", pageWidth / 2f, 680f, paint)
        canvas.drawText("2. Наведіть камеру на QR-код вище", pageWidth / 2f, 700f, paint)
        canvas.drawText("3. Вкажіть суму вашого замовлення та підтвердіть оплату в додатку банку", pageWidth / 2f, 720f, paint)

        // 8. Підвал
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("uk-UA")).format(Date())
        canvas.drawText("Згенеровано через QR POS Термінал • $dateStr • ${merchant.tagline}", pageWidth / 2f, 790f, paint)

        pdfDocument.finishPage(page)

        // Збереження у кеш для шерингу
        val outputFile = File(context.cacheDir, "qr_pos_poster_${merchant.merchantId}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    /**
     * Відкриває системне діалогове вікно шерингу / друку згенерованого PDF-постера.
     */
    fun shareOrPrintPoster(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Фірмовий постер QR POS для друку")
            putExtra(Intent.EXTRA_TEXT, "Друкований тейбл-тент з QR-кодом для прийому оплат.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Друк або надсилання постера"))
    }
}
