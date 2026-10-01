package com.example.model

enum class QrPaymentMode(
    val title: String,
    val subtitle: String,
    val instruction: String,
    val iconEmoji: String
) {
    WEB_LINK(
        title = "Камера",
        subtitle = "Універсальний Web-міст",
        instruction = "📸 Скануйте звичайною камерою iPhone або Android. Відкриється сторінка швидкої оплати з вибором вашого банку (Monobank, Privat24, ПУМБ, Sense тощо) або оплати карткою.",
        iconEmoji = "📱"
    ),
    NBU_STANDARD(
        title = "НБУ QR",
        subtitle = "Стандарт НБУ (Усі банки)",
        instruction = "🏦 Відкрийте додаток вашого банку (Monobank, Приват24, ПУМБ, Sense, Ощад тощо) → оберіть «Сканер QR» → підтвердіть платіж. Сума та IBAN заповняться автоматично.",
        iconEmoji = "🏛️"
    )
}