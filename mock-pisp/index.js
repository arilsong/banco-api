const express = require('express');
const axios = require('axios');
const app = express();
const port = 4015;

app.use(express.json());

// Armazenar logs em memória para visualizar o flow
let logs = [];

function logAction(action, data) {
    const logEntry = {
        timestamp: new Date().toISOString(),
        action,
        data
    };
    logs.push(logEntry);
    console.log(`[${logEntry.timestamp}] ${action}:`, JSON.stringify(data, null, 2));
}

// ─── ENDPOINTS DO MOCK PISP (Switch Mock) ────────────────────────────────────

// GET /logs - Ver o que está a acontecer
app.get('/logs', (req, res) => {
    res.json(logs);
});

// GET / - Dashboard simples em HTML
app.get('/', (req, res) => {
    let html = `
        <html>
        <head>
            <title>Mock PISP Dashboard</title>
            <style>
                body { font-family: sans-serif; background: #f0f2f5; padding: 20px; }
                .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }
                pre { background: #eee; padding: 10px; border-radius: 4px; overflow-x: auto; }
                .btn { background: #007bff; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; text-decoration: none; display: inline-block; }
                .log-entry { border-bottom: 1px solid #ddd; padding: 10px 0; }
                .action { font-weight: bold; color: #007bff; }
            </style>
        </head>
        <body>
            <h1>Mock PISP / Switch Mojaloop</h1>
            <div class="card">
                <h2>Fase 1: Discovery (Listar Contas)</h2>
                <button class="btn" onclick="triggerAccounts()">1. Listar Contas (BCA)</button>
            </div>
            <div class="card">
                <h2>Fase 2: Linking (Consentimento)</h2>
                <button class="btn" onclick="triggerBcaConsent()" style="background: #28a745;">2. Iniciar Consentimento BCA</button>
            </div>
            <div class="card">
                <h2>Logs de Rede</h2>
                <div id="logs">
                    ${logs.slice().reverse().map(l => `
                        <div class="log-entry">
                            <span class="action">${l.action}</span> - ${l.timestamp}<br>
                            <pre>${JSON.stringify(l.data, null, 2)}</pre>
                        </div>
                    `).join('')}
                </div>
            </div>
            <script>
                async function triggerAccounts() {
                    const res = await fetch('/trigger/accounts/2389389274');
                    alert(await res.text());
                    location.reload();
                }
                async function triggerBcaConsent() {
                    const res = await fetch('/trigger/bca');
                    alert(await res.text());
                    location.reload();
                }
                // Refresh automático
                setTimeout(() => location.reload(), 5000);
            </script>
        </body>
        </html>
    `;
    res.send(html);
});

// ─── SIMULAÇÃO DE TRIGGER (PISP chamando DFSP) ───────────────────────────────

app.get('/trigger/accounts/:userId', async (req, res) => {
    const userId = req.params.userId;
    const dfspUrl = 'http://localhost:8081'; // BCA
    
    logAction(`Iniciando Account Discovery para ${userId}`, { url: `${dfspUrl}/accounts/${userId}` });

    try {
        await axios.get(`${dfspUrl}/accounts/${userId}`, {
            headers: { 'FSPIOP-Source': 'meu-pisp' }
        });
        res.send(`Account Discovery iniciado para ${userId}`);
    } catch (error) {
        logAction(`Erro no Discovery`, error.message);
        res.status(500).send(`Erro: ${error.message}`);
    }
});

app.get('/trigger/:bank', async (req, res) => {
    const bank = req.params.bank;
    const dfspUrl = bank === 'bca' ? 'http://localhost:8081' : 'http://localhost:8082';
    const consentRequestId = `req-${Date.now()}`;
    
    const body = {
        consentRequestId: consentRequestId,
        userId: "2389389274",
        scopes: [
            {
                accountId: "2389389274",
                actions: ["ACCOUNTS_GET_BALANCE", "ACCOUNTS_TRANSFER"]
            }
        ],
        authChannels: ["OTP"],
        callbackUri: `http://localhost:${port}`
    };

    logAction(`Iniciando ConsentRequest no ${bank.toUpperCase()}`, { url: `${dfspUrl}/consentRequests`, body });

    try {
        await axios.post(`${dfspUrl}/consentRequests`, body, {
            headers: { 'FSPIOP-Source': 'meu-pisp' }
        });
        res.send(`ConsentRequest iniciado no ${bank.toUpperCase()}! ID: ${consentRequestId}`);
    } catch (error) {
        logAction(`Erro ao iniciar no ${bank.toUpperCase()}`, error.message);
        res.status(500).send(`Erro: ${error.message}`);
    }
});

// ─── RECEBENDO CALLBACKS (Simulando o Switch) ────────────────────────────────

// PUT /accounts/{userId}
app.put('/accounts/:userId', (req, res) => {
    logAction(`Recebido PUT /accounts/${req.params.userId}`, {
        headers: req.headers,
        body: req.body
    });
    res.status(202).send();
});

// PUT /consentRequests/{ID}
app.put('/consentRequests/:id', (req, res) => {
    logAction(`Recebido PUT /consentRequests/${req.params.id}`, {
        headers: req.headers,
        body: req.body
    });
    res.status(202).send();
});

// POST /consents
app.post('/consents', (req, res) => {
    logAction(`Recebido POST /consents`, {
        headers: req.headers,
        body: req.body
    });
    res.status(202).send();
});

// PATCH /consents/{id}
app.patch('/consents/:id', (req, res) => {
    logAction(`Recebido PATCH /consents/${req.params.id}`, {
        headers: req.headers,
        body: req.body
    });
    res.status(202).send();
});

app.listen(port, () => {
    console.log(`Mock PISP rodando em http://localhost:${port}`);
});
