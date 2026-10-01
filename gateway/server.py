"""
QR-POS Universal Bank Webhook Gateway & Mobile Payment Bridge
Python 3.13 Standalone Server (Zero External Dependencies)

Usage:
    python server.py [port]
"""

import sys
import os
import json
import time
import html
import urllib.parse
from http.server import HTTPServer, BaseHTTPRequestHandler
import socketserver
import threading
import queue

try:
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    if hasattr(sys.stderr, 'reconfigure'):
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else int(os.environ.get("PORT", 8787))

# In-memory SSE subscribers: merchant_id -> list of Queue objects
subscribers_lock = threading.Lock()
subscribers = {}

def add_subscriber(merchant_id, q):
    with subscribers_lock:
        if merchant_id not in subscribers:
            subscribers[merchant_id] = []
        subscribers[merchant_id].append(q)

def remove_subscriber(merchant_id, q):
    with subscribers_lock:
        if merchant_id in subscribers:
            try:
                subscribers[merchant_id].remove(q)
                if not subscribers[merchant_id]:
                    del subscribers[merchant_id]
            except ValueError:
                pass

def broadcast_to_merchant(merchant_id, event_data):
    payload = f"data: {json.dumps(event_data, ensure_ascii=False)}\n\n"
    count = 0
    with subscribers_lock:
        targets = list(subscribers.get(merchant_id, []))
    for q in targets:
        try:
            q.put_nowait(payload)
            count += 1
        except Exception:
            pass
    return count

def generate_payment_html(to_name, iban, amount, purpose, card):
    safe_to = html.escape(to_name)
    safe_iban = html.escape(iban)
    safe_amount = html.escape(amount)
    safe_purpose = html.escape(purpose)
    safe_card = html.escape(card)
    
    privat_fallback = f"https://next.privat24.ua/money-transfer/card?recipient={safe_card}&amount={safe_amount}" if card else "https://next.privat24.ua"

    return f"""<!DOCTYPE html>
<html lang="uk">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Оплата {safe_amount} ₴ • QR POS</title>
  <style>
    * {{ box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }}
    body {{ background: #0B132B; color: #FFFFFF; min-height: 100vh; display: flex; flex-direction: column; align-items: center; padding: 20px 16px; }}
    .card {{ background: #1C2541; border-radius: 24px; padding: 24px 20px; width: 100%; max-width: 440px; box-shadow: 0 12px 36px rgba(0,0,0,0.4); border: 1px solid rgba(255,255,255,0.08); text-align: center; }}
    .badge {{ display: inline-flex; align-items: center; gap: 6px; background: rgba(16,185,129,0.15); color: #34D399; padding: 6px 14px; border-radius: 99px; font-size: 13px; font-weight: 600; margin-bottom: 16px; }}
    .amount {{ font-size: 42px; font-weight: 900; color: #FBBF24; letter-spacing: -0.5px; margin-bottom: 4px; }}
    .merchant {{ font-size: 18px; font-weight: 700; color: #FFFFFF; margin-bottom: 4px; }}
    .purpose {{ font-size: 13px; color: #94A3B8; margin-bottom: 20px; word-break: break-word; }}
    .divider {{ height: 1px; background: rgba(255,255,255,0.08); margin: 18px 0; }}
    .btn {{ display: flex; align-items: center; justify-content: center; gap: 10px; width: 100%; padding: 14px 16px; border-radius: 16px; text-decoration: none; font-size: 15px; font-weight: 700; margin-bottom: 10px; cursor: pointer; transition: all 0.2s ease; border: none; }}
    .btn-mono {{ background: #18181B; color: #FFFFFF; border: 1px solid #E11D48; }}
    .btn-privat {{ background: #15803D; color: #FFFFFF; }}
    .btn-pumb {{ background: #B91C1C; color: #FFFFFF; }}
    .btn-sense {{ background: #0284C7; color: #FFFFFF; }}
    .btn-abank {{ background: #16A34A; color: #FACC15; }}
    .btn-copy {{ background: rgba(255,255,255,0.08); color: #38BDF8; border: 1px dashed rgba(56,189,248,0.5); }}
    .btn:active {{ transform: scale(0.98); opacity: 0.9; }}
    .iban-box {{ background: rgba(0,0,0,0.25); border-radius: 12px; padding: 10px; margin-top: 14px; font-family: monospace; font-size: 12px; color: #CBD5E1; word-break: break-all; }}
    .toast {{ position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: #10B981; color: #000; padding: 10px 20px; border-radius: 99px; font-weight: 700; font-size: 14px; display: none; box-shadow: 0 8px 20px rgba(0,0,0,0.5); z-index: 100; }}
    .footer {{ margin-top: 24px; font-size: 12px; color: #64748B; text-align: center; }}
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">🇺🇦 Швидка банківська оплата</div>
    <div class="amount">{safe_amount} ₴</div>
    <div class="merchant">{safe_to}</div>
    <div class="purpose">{safe_purpose}</div>

    <div class="divider"></div>
    <div style="font-size: 13px; color: #94A3B8; font-weight: 600; margin-bottom: 12px; text-align: left;">Оберіть ваш мобільний банк:</div>

    <a href="monobank://send" class="btn btn-mono" onclick="fallbackLink(event, 'https://send.monobank.ua')">
      ⬛ Відкрити в Monobank
    </a>

    <a href="privat24://pay" class="btn btn-privat" onclick="fallbackLink(event, '{privat_fallback}')">
      🟩 Відкрити в Приват24
    </a>

    <a href="mybis://transfer" class="btn btn-pumb" onclick="fallbackLink(event, 'https://www.pumb.ua/p2p')">
      🟥 Відкрити в ПУМБ (MyПУМБ)
    </a>

    <a href="sense://pay" class="btn btn-sense" onclick="fallbackLink(event, 'https://sensebank.ua/perevod-s-karty-na-kartu')">
      🟦 Відкрити в Sense SuperApp
    </a>

    <a href="abank24://transfer" class="btn btn-abank" onclick="fallbackLink(event, 'https://a-bank.com.ua/transfers')">
      🟨 Відкрити в А-Банк (ABank24)
    </a>

    <button class="btn btn-copy" onclick="copyIban('{safe_iban}')">
      📋 Скопіювати IBAN для платежу
    </button>

    {'<div class="iban-box">IBAN: ' + safe_iban + '</div>' if safe_iban else ''}
  </div>

  <div id="toast" class="toast">✅ IBAN скопійовано! Вставте у свій банк</div>
  <div class="footer">QR POS Terminal • Миттєві перекази СЕП НБУ 0%</div>

  <script>
    function copyIban(text) {{
      if (!text) return;
      navigator.clipboard.writeText(text).then(() => {{
        showToast();
      }}).catch(() => {{
        const el = document.createElement('textarea');
        el.value = text;
        document.body.appendChild(el);
        el.select();
        document.execCommand('copy');
        document.body.removeChild(el);
        showToast();
      }});
    }}

    function showToast() {{
      const toast = document.getElementById('toast');
      toast.style.display = 'block';
      setTimeout(() => {{ toast.style.display = 'none'; }}, 2400);
    }}

    function fallbackLink(event, fallbackUrl) {{
      setTimeout(() => {{
        window.location.href = fallbackUrl;
      }}, 700);
    }}
  </script>
</body>
</html>"""

class GatewayHandler(BaseHTTPRequestHandler):
    def send_cors_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Token')

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_cors_headers()
        self.end_headers()

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        query = urllib.parse.parse_qs(parsed.query)

        # Health
        if path in ('/', '/health'):
            res = json.dumps({
                "service": "QR-POS Universal Webhook Gateway",
                "status": "online",
                "timestamp": int(time.time() * 1000),
                "version": "1.0.0"
            }).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_cors_headers()
            self.send_header('Content-Length', str(len(res)))
            self.end_headers()
            self.wfile.write(res)
            return

        # /pay Mobile Payment Bridge
        if path == '/pay':
            to_name = query.get('to', ['ФОП Одержувач'])[0]
            iban = query.get('iban', [''])[0].replace(' ', '')
            amount = query.get('amount', ['0.00'])[0]
            purpose = query.get('purpose', ['Оплата замовлення'])[0]
            card = query.get('card', [''])[0].replace(' ', '')

            content = generate_payment_html(to_name, iban, amount, purpose, card).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.send_cors_headers()
            self.send_header('Content-Length', str(len(content)))
            self.end_headers()
            self.wfile.write(content)
            return

        # SSE Stream: /events/<merchantId>
        if path.startswith('/events/'):
            merchant_id = path[len('/events/'):].strip()
            if not merchant_id:
                self.send_error(400, "Missing merchantId")
                return

            self.send_response(200)
            self.send_header('Content-Type', 'text/event-stream')
            self.send_header('Cache-Control', 'no-cache, no-transform')
            self.send_header('Connection', 'keep-alive')
            self.send_cors_headers()
            self.end_headers()

            q = queue.Queue()
            add_subscriber(merchant_id, q)
            # Send initial greeting
            welcome = f": connected to qr-pos gateway for {merchant_id}\n\n".encode('utf-8')
            try:
                self.wfile.write(welcome)
                self.wfile.flush()
                
                while True:
                    try:
                        msg = q.get(timeout=25.0)
                        self.wfile.write(msg.encode('utf-8'))
                        self.wfile.flush()
                    except queue.Empty:
                        # Keepalive ping
                        self.wfile.write(b": ping\n\n")
                        self.wfile.flush()
            except (ConnectionResetError, BrokenPipeError):
                pass
            finally:
                remove_subscriber(merchant_id, q)
            return

        self.send_error(404, "Not Found")

    def do_POST(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path

        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length) if content_length > 0 else b'{}'
        
        try:
            body = json.loads(post_data.decode('utf-8')) if post_data else {}
        except Exception:
            body = {}

        # 1. Monobank Webhook: /webhook/mono/<merchantId>
        if path.startswith('/webhook/mono/'):
            merchant_id = path[len('/webhook/mono/'):].strip()
            amount = 0.0
            tx_id = f"mono-{int(time.time() * 1000)}"
            comment = "Оплата через Monobank"

            if body.get('type') == 'StatementItem' and 'data' in body and 'statementItem' in body['data']:
                item = body['data']['statementItem']
                amount = (item.get('amount', 0)) / 100.0
                tx_id = item.get('id', tx_id)
                comment = item.get('description', comment)
            elif 'amount' in body:
                amount = float(body['amount'])

            normalized = {
                "event": "PAYMENT_RECEIVED",
                "merchantId": merchant_id,
                "amount": amount,
                "currency": "UAH",
                "bank": "MONOBANK",
                "transactionId": tx_id,
                "comment": comment,
                "timestamp": int(time.time() * 1000)
            }

            delivered = broadcast_to_merchant(merchant_id, normalized)
            res = json.dumps({"status": "ok", "received": True, "amount": amount, "deliveredToClients": delivered}).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_cors_headers()
            self.send_header('Content-Length', str(len(res)))
            self.end_headers()
            self.wfile.write(res)
            return

        # 2. PrivatBank Webhook: /webhook/privat/<merchantId>
        if path.startswith('/webhook/privat/'):
            merchant_id = path[len('/webhook/privat/'):].strip()
            amount = float(body.get('amount') or body.get('sum') or 0.0)
            tx_id = body.get('payment_id') or body.get('id') or f"pb-{int(time.time() * 1000)}"
            comment = body.get('description') or body.get('purpose') or "Оплата через ПриватБанк"

            normalized = {
                "event": "PAYMENT_RECEIVED",
                "merchantId": merchant_id,
                "amount": amount,
                "currency": "UAH",
                "bank": "PRIVATBANK",
                "transactionId": tx_id,
                "comment": comment,
                "timestamp": int(time.time() * 1000)
            }

            delivered = broadcast_to_merchant(merchant_id, normalized)
            res = json.dumps({"status": "ok", "deliveredToClients": delivered}).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_cors_headers()
            self.send_header('Content-Length', str(len(res)))
            self.end_headers()
            self.wfile.write(res)
            return

        # 3. Test Push: /test-push/<merchantId>
        if path.startswith('/test-push/'):
            merchant_id = path[len('/test-push/'):].strip()
            amount = float(body.get('amount', 150.0))
            bank = body.get('bank', 'MONOBANK')

            test_event = {
                "event": "PAYMENT_RECEIVED",
                "merchantId": merchant_id,
                "amount": amount,
                "currency": "UAH",
                "bank": bank,
                "transactionId": f"sim-{str(int(time.time() * 1000))[-6:]}",
                "comment": body.get('comment', 'Тестовий платіж через Webhook Gateway'),
                "timestamp": int(time.time() * 1000)
            }

            delivered = broadcast_to_merchant(merchant_id, test_event)
            res = json.dumps({
                "status": "ok",
                "message": f"Тестова оплата {amount} ₴ відправлена",
                "deliveredToClients": delivered
            }).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_cors_headers()
            self.send_header('Content-Length', str(len(res)))
            self.end_headers()
            self.wfile.write(res)
            return

        self.send_error(404, "Not Found")

    def log_message(self, format, *args):
        # Concise logging
        print(f"[{time.strftime('%H:%M:%S')}] {format % args}")

class ThreadingHTTPServer(socketserver.ThreadingMixIn, HTTPServer):
    daemon_threads = True

if __name__ == '__main__':
    server = ThreadingHTTPServer(('0.0.0.0', PORT), GatewayHandler)
    print(f"🚀 QR-POS Webhook Gateway & Payment Bridge running at http://localhost:{PORT}")
    print(f"   - Health check:     http://localhost:{PORT}/health")
    print(f"   - Payment Page:     http://localhost:{PORT}/pay?amount=150&iban=UA123...&to=FOP")
    print(f"   - Live SSE Stream:  http://localhost:{PORT}/events/<merchantId>")
    print(f"   - Monobank Webhook: http://localhost:{PORT}/webhook/mono/<merchantId>")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping gateway server...")
        server.server_close()
