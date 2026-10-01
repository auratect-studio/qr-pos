/**
 * Standalone Node.js Runner for local testing / Docker / VPS
 * Usage: node server.js
 */
const http = require('http');
const workerModule = require('./worker.js').default;

const PORT = process.env.PORT || 8787;

const server = http.createServer(async (req, res) => {
  const url = 'http://' + (req.headers.host || 'localhost:' + PORT) + req.url;
  
  let bodyChunks = [];
  req.on('data', chunk => bodyChunks.push(chunk));
  req.on('end', async () => {
    const rawBody = Buffer.concat(bodyChunks);
    const request = new Request(url, {
      method: req.method,
      headers: req.headers,
      body: ['GET', 'HEAD'].includes(req.method) ? undefined : rawBody
    });

    try {
      const response = await workerModule.fetch(request, {}, {});
      res.statusCode = response.status;
      for (const [k, v] of response.headers.entries()) {
        res.setHeader(k, v);
      }
      
      if (response.body) {
        const reader = response.body.getReader();
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          res.write(value);
        }
      }
      res.end();
    } catch (err) {
      res.statusCode = 500;
      res.end('Internal Server Error: ' + err.message);
    }
  });
});

server.listen(PORT, () => {
  console.log(`🚀 QR-POS Webhook Gateway running at http://localhost:${PORT}`);
});