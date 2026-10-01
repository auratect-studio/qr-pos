# 🚀 Інструкція з розгортання платіжного мосту QR-POS Gateway

Шлюз **QR-POS Gateway** виконує дві ключові ролі:
1. **Universal Mobile Payment Bridge (`/pay`)**: веб-сторінка швидкої оплати при скануванні QR-коду звичайною камерою смартфона клієнта (iPhone / Android) з прямим переходом у Monobank, Приват24, ПУМБ, Sense Bank, А-Банк або копіюванням IBAN.
2. **Webhook & Live SSE Hub (`/webhook/*`, `/events/*`)**: прийом сповіщень про надходження грошей від банків у режимі реального часу та миттєва передача в додаток QR-POS на касі.

---

## 🟢 Варіант 1. Локальний сервер (Вже запущено та працює)

Локальний сервер на базі Python 3.13 (`server.py`) не потребує сторонніх бібліотек чи Node.js.

### Команди керування:
```powershell
# Запуск шлюзу на порті 8787:
python gateway/server.py 8787
```

### Доступні ендпоінти:
- **Перевірка стану**: `http://localhost:8787/health`
- **Платіжна сторінка**: `http://localhost:8787/pay?amount=150.00&iban=UA...&to=ФОП`
- **SSE-потік для каси**: `http://localhost:8787/events/fop-demo`
- **Monobank Webhook**: `http://localhost:8787/webhook/mono/fop-demo`
- **Тестова оплата**: `http://localhost:8787/test-push/fop-demo`

---

## ☁️ Варіант 2. Розгортання у Cloudflare Workers (Рекомендовано для Production)

Безкоштовний тариф Cloudflare дає до **100 000 запитів на добу**, нульову затримку в Україні (Anycast DNS) та захист від DDoS.

### Спосіб А. Через вебінтерфейс Cloudflare (без встановлення Node.js / CLI) — 2 хвилини:
1. Зареєструйтесь або увійдіть на [dash.cloudflare.com](https://dash.cloudflare.com/).
2. Перейдіть у лівому меню: **Compute (Workers & Pages)** ➡️ **Create application** ➡️ **Create Worker**.
3. Вкажіть ім'я воркера: `qr-pos-gateway` та натисніть **Deploy**.
4. Натисніть **Edit code** у правому верхньому кутку.
5. Замініть вміст файлу `worker.js` кодом із нашого локального файлу [`gateway/worker.js`](file:///C:/Users/admin/.gemini/antigravity/scratch/qr-pos/gateway/worker.js).
6. Натисніть **Deploy**.
7. Скопіюйте отриману адресу (наприклад, `https://qr-pos-gateway.<your-subdomain>.workers.dev`) та вставте її у налаштуваннях профілю додатку QR-POS (розділ 5: «URL шлюзу»).

### Спосіб Б. Через командний рядок Wrangler:
Якщо встановити Node.js (`winget install OpenJS.NodeJS.LTS`):
```powershell
cd C:\Users\admin\.gemini\antigravity\scratch\qr-pos\gateway
npx wrangler login
npx wrangler deploy
```

---

## 🚇 Варіант 3. Безкоштовний публічний тунель Cloudflared (для тестування з телефону)

Якщо ви бажаєте, щоб локальний сервер на вашому ПК був моментально доступний з будь-якого мобільного телефону в інтернеті:
```powershell
# 1. Встановити cloudflared (якщо ще не встановлено)
winget install Cloudflare.cloudflared

# 2. Запустити безкоштовний тимчасовий тунель
cloudflared tunnel --url http://localhost:8787
```
Cloudflare видасть посилання вигляду: `https://random-subdomain.trycloudflare.com`.  
Вкажіть його в налаштуваннях додатку, і ваш телефон одразу зв'яжеться з касою!
