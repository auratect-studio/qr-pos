/**
 * QR-POS Universal Bank Webhook Gateway
 * Cloudflare Worker / Edge Runtime compatible
 * 
 * Supports:
 * - Monobank Personal & Acquiring Webhooks
 * - PrivatBank / LiqPay / Autoclient Callbacks
 * - Generic Ukrainian Bank Webhooks
 * - SSE (Server-Sent Events) live streaming to Android QR-POS client
 */

// In-memory SSE connections map: merchantId -> Set of Response controllers
const subscribers = new Map();

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function addSubscriber(merchantId, controller) {
  if (!subscribers.has(merchantId)) {
    subscribers.set(merchantId, new Set());
  }
  subscribers.get(merchantId).add(controller);
}

function removeSubscriber(merchantId, controller) {
  if (subscribers.has(merchantId)) {
    const set = subscribers.get(merchantId);
    set.delete(controller);
    if (set.size === 0) {
      subscribers.delete(merchantId);
    }
  }
}

function broadcastToMerchant(merchantId, eventData) {
  if (!subscribers.has(merchantId)) return 0;
  const set = subscribers.get(merchantId);
  const payload = `data: ${JSON.stringify(eventData)}\n\n`;
  const encoder = new TextEncoder();
  const bytes = encoder.encode(payload);

  let deliveredCount = 0;
  for (const controller of set) {
    try {
      controller.enqueue(bytes);
      deliveredCount++;
    } catch (e) {
      set.delete(controller);
    }
  }
  return deliveredCount;
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;

    // CORS preflight
    if (method === 'OPTIONS') {
      return new Response(null, {
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Token'
        }
      });
    }

    // Health check
    if (path === '/' || path === '/health') {
      return new Response(JSON.stringify({
        service: 'QR-POS Universal Webhook Gateway',
        status: 'online',
        timestamp: Date.now(),
        version: '1.0.0'
      }), {
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
      });
    }

    // 0. Universal Mobile Payment Bridge: GET /pay
    if (method === 'GET' && path === '/pay') {
      const to = url.searchParams.get('to') || 'ФОП Одержувач';
      const iban = (url.searchParams.get('iban') || '').replace(/\s+/g, '');
      const amount = url.searchParams.get('amount') || '0.00';
      const purpose = url.searchParams.get('purpose') || 'Оплата замовлення';
      const bank = url.searchParams.get('bank') || 'mono';
      const card = (url.searchParams.get('card') || '').replace(/\s+/g, '');
      const pumbUrl = url.searchParams.get('pumb_url') || 'https://mobile-app.pumb.ua/1YAsa';

      const html = `<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Оплата ${amount} ₴ • QR POS</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
    body { background: #0B132B; color: #FFFFFF; min-height: 100vh; display: flex; flex-direction: column; align-items: center; padding: 20px 16px; }
    .card { background: #1C2541; border-radius: 24px; padding: 24px 20px; width: 100%; max-width: 440px; box-shadow: 0 12px 36px rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.08); text-align: center; }
    .badge { display: inline-flex; align-items: center; gap: 6px; background: rgba(16,185,129,0.15); color: #34D399; padding: 6px 14px; border-radius: 99px; font-size: 13px; font-weight: 600; margin-bottom: 16px; }
    .amount { font-size: 42px; font-weight: 900; color: #FBBF24; letter-spacing: -0.5px; margin-bottom: 4px; }
    .merchant { font-size: 18px; font-weight: 700; color: #FFFFFF; margin-bottom: 4px; }
    .purpose { font-size: 13px; color: #94A3B8; margin-bottom: 20px; word-break: break-word; }
    .divider { height: 1px; background: rgba(255,255,255,0.08); margin: 18px 0; }
    .btn { display: flex; align-items: center; justify-content: center; gap: 10px; width: 100%; padding: 14px 16px; border-radius: 16px; text-decoration: none; font-size: 15px; font-weight: 700; margin-bottom: 10px; cursor: pointer; transition: all 0.2s ease; border: none; }
    .btn-mono { background: #18181B; color: #FFFFFF; border: 1px solid #E11D48; }
    .btn-privat { background: #15803D; color: #FFFFFF; }
    .btn-pumb { background: #B91C1C; color: #FFFFFF; }
    .btn-sense { background: #0284C7; color: #FFFFFF; }
    .btn-abank { background: #16A34A; color: #FACC15; }
    .btn-copy { background: rgba(255,255,255,0.08); color: #38BDF8; border: 1px dashed rgba(56,189,248,0.5); }
    .btn:active { transform: scale(0.98); opacity: 0.9; }
    .iban-box { background: rgba(0,0,0,0.25); border-radius: 12px; padding: 10px; margin-top: 14px; font-family: monospace; font-size: 12px; color: #CBD5E1; word-break: break-all; }
    .toast { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: #10B981; color: #000; padding: 10px 20px; border-radius: 99px; font-weight: 700; font-size: 14px; display: none; box-shadow: 0 8px 20px rgba(0,0,0,0.5); z-index: 100; }
    .footer { margin-top: 24px; font-size: 12px; color: #64748B; text-align: center; }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">🇺🇦 Швидка банківська оплата</div>
    <div class="amount">${escapeHtml(amount)} ₴</div>
    <div class="merchant">${escapeHtml(to)}</div>
    <div class="purpose">${escapeHtml(purpose)}</div>

    <div class="divider"></div>
    <div style="font-size: 13px; color: #94A3B8; font-weight: 600; margin-bottom: 12px; text-align: left;">Оберіть ваш мобільний банк:</div>

    <a href="monobank://send" class="btn btn-mono" onclick="fallbackLink(event, 'https://send.monobank.ua')">
      ⬛ Відкрити в Monobank
    </a>

    <a href="privat24://pay" class="btn btn-privat" onclick="fallbackLink(event, '${card ? 'https://next.privat24.ua/money-transfer/card?recipient=' + card + '&amount=' + amount : 'https://next.privat24.ua'}')">
      🟩 Відкрити в Приват24
    </a>

    <a href="${pumbUrl}" class="btn btn-pumb" onclick="fallbackLink(event, '${pumbUrl}')">
      🟥 Відкрити в ПУМБ (МаніБокс / MyПУМБ)
    </a>

    <a href="sense://pay" class="btn btn-sense" onclick="fallbackLink(event, 'https://sensebank.ua/perevod-s-karty-na-kartu')">
      🟦 Відкрити в Sense SuperApp
    </a>

    <a href="abank24://transfer" class="btn btn-abank" onclick="fallbackLink(event, 'https://a-bank.com.ua/transfers')">
      🟨 Відкрити в А-Банк (ABank24)
    </a>

    <button class="btn btn-copy" onclick="copyIban('${escapeHtml(iban)}')">
      📋 Скопіювати IBAN для платежу
    </button>

    ${iban ? `<div class="iban-box">IBAN: ${escapeHtml(iban)}</div>` : ''}
  </div>

  <div id="toast" class="toast">✅ IBAN скопійовано! Вставте у свій банк</div>
  <div class="footer">QR POS Terminal • Миттєві перекази СЕП НБУ 0%</div>

  <script>
    function copyIban(text) {
      if (!text) return;
      navigator.clipboard.writeText(text).then(() => {
        showToast();
      }).catch(() => {
        const el = document.createElement('textarea');
        el.value = text;
        document.body.appendChild(el);
        el.select();
        document.execCommand('copy');
        document.body.removeChild(el);
        showToast();
      });
    }

    function showToast() {
      const toast = document.getElementById('toast');
      toast.style.display = 'block';
      setTimeout(() => { toast.style.display = 'none'; }, 2400);
    }

    function fallbackLink(event, fallbackUrl) {
      const targetSchemeUrl = event.currentTarget.getAttribute('href');
      setTimeout(() => {
        window.location.href = fallbackUrl;
      }, 700);
    }
  </script>
</body>
</html>`;

      return new Response(html, {
        headers: {
          'Content-Type': 'text/html; charset=utf-8',
          'Access-Control-Allow-Origin': '*'
        }
      });
    }

    // 1. SSE Stream for Android App: GET /events/:merchantId
    if (method === 'GET' && path.startsWith('/events/')) {
      const merchantId = path.replace('/events/', '').trim();
      if (!merchantId) {
        return new Response('Missing merchantId', { status: 400 });
      }

      let clientController = null;
      const stream = new ReadableStream({
        start(controller) {
          clientController = controller;
          addSubscriber(merchantId, controller);
          // Initial greeting and ping
          const encoder = new TextEncoder();
          controller.enqueue(encoder.encode(`: connected to qr-pos gateway for ${merchantId}\n\n`));
        },
        cancel() {
          if (clientController) {
            removeSubscriber(merchantId, clientController);
          }
        }
      });

      return new Response(stream, {
        headers: {
          'Content-Type': 'text/event-stream',
          'Cache-Control': 'no-cache, no-transform',
          'Connection': 'keep-alive',
          'Access-Control-Allow-Origin': '*'
        }
      });
    }

    // 2. Monobank Webhook: POST /webhook/mono/:merchantId
    if (method === 'POST' && path.startsWith('/webhook/mono/')) {
      const merchantId = path.replace('/webhook/mono/', '').trim();
      try {
        const body = await request.json();
        
        // Monobank statement item structure:
        // { type: "StatementItem", data: { account: "...", statementItem: { id, time, amount, description ... } } }
        let amount = 0;
        let txId = 'mono-' + Date.now();
        let comment = 'Оплата через Monobank';

        if (body.type === 'StatementItem' && body.data && body.data.statementItem) {
          const item = body.data.statementItem;
          // Monobank sends amount in kopecks (e.g. 15000 = 150.00 UAH)
          amount = (item.amount || 0) / 100.0;
          txId = item.id || txId;
          comment = item.description || comment;
        } else if (body.amount) {
          amount = parseFloat(body.amount);
        }

        const normalizedEvent = {
          event: 'PAYMENT_RECEIVED',
          merchantId: merchantId,
          amount: amount,
          currency: 'UAH',
          bank: 'MONOBANK',
          transactionId: txId,
          comment: comment,
          timestamp: Date.now()
        };

        const delivered = broadcastToMerchant(merchantId, normalizedEvent);

        return new Response(JSON.stringify({
          status: 'ok',
          received: true,
          amount: amount,
          deliveredToClients: delivered
        }), {
          headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
        });
      } catch (e) {
        return new Response(JSON.stringify({ error: e.message }), { status: 400 });
      }
    }

    // 3. PrivatBank / LiqPay Webhook: POST /webhook/privat/:merchantId
    if (method === 'POST' && path.startsWith('/webhook/privat/')) {
      const merchantId = path.replace('/webhook/privat/', '').trim();
      try {
        const body = await request.json();
        const amount = parseFloat(body.amount || body.sum || 0);
        const txId = body.payment_id || body.id || ('pb-' + Date.now());
        const comment = body.description || body.purpose || 'Оплата через ПриватБанк';

        const normalizedEvent = {
          event: 'PAYMENT_RECEIVED',
          merchantId: merchantId,
          amount: amount,
          currency: 'UAH',
          bank: 'PRIVATBANK',
          transactionId: txId,
          comment: comment,
          timestamp: Date.now()
        };

        const delivered = broadcastToMerchant(merchantId, normalizedEvent);
        return new Response(JSON.stringify({ status: 'ok', deliveredToClients: delivered }), {
          headers: { 'Content-Type': 'application/json' }
        });
      } catch (e) {
        return new Response(JSON.stringify({ error: e.message }), { status: 400 });
      }
    }

    // 4. Test / Simulator Push: POST /test-push/:merchantId
    if (method === 'POST' && path.startsWith('/test-push/')) {
      const merchantId = path.replace('/test-push/', '').trim();
      try {
        const body = await request.json().catch(() => ({}));
        const amount = parseFloat(body.amount || 150.0);
        const bank = body.bank || 'MONOBANK';

        const testEvent = {
          event: 'PAYMENT_RECEIVED',
          merchantId: merchantId,
          amount: amount,
          currency: 'UAH',
          bank: bank,
          transactionId: 'sim-' + Date.now().toString().slice(-6),
          comment: body.comment || 'Тестовий платіж через Webhook Gateway',
          timestamp: Date.now()
        };

        const delivered = broadcastToMerchant(merchantId, testEvent);
        return new Response(JSON.stringify({
          status: 'ok',
          message: `Тестова оплата ${amount} ₴ відправлена`,
          deliveredToClients: delivered
        }), {
          headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }
        });
      } catch (e) {
        return new Response(JSON.stringify({ error: e.message }), { status: 400 });
      }
    }

    return new Response('Not Found', { status: 404 });
  }
};