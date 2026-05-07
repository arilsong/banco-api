const express = require('express');
const app = express();
const port = process.env.PORT || 8081;

app.use(express.json({ type: '*/*' }));

console.log(`[Mock Backend] Iniciando Simulador Core na porta ${port}...`);

// --- BASE DE DADOS EM MEMÓRIA ---
const accounts = [
    {
        accountNickname: "Conta Arilson (Mock)",
        id: "bca.msisdn.2389389274",
        currency: "CVE",
        address: "bca.msisdn.2389389274"
    },
    {
        accountNickname: "Poupança Mock",
        id: "bca.msisdn.999112233",
        currency: "CVE",
        address: "bca.msisdn.999112233"
    }
];

// --- ENDPOINTS PARA O THIRDPARTY-SDK ---

/**
 * 1. GET /accounts/{userId}
 */
app.get('/accounts/:userId', (req, res) => {
    const { userId } = req.params;
    console.log(`[Mock Backend] GET /accounts/${userId}`);
    
    // Filtra contas se o userId bater, senão retorna as padrão
    const result = accounts.filter(a => a.id.includes(userId));
    res.json({ accounts: result.length > 0 ? result : accounts });
});

/**
 * 2. POST /consentRequests
 */
app.post('/consentRequests', (req, res) => {
    const { consentRequestId, userId } = req.body;
    console.log(`[Mock Backend] POST /consentRequests id=${consentRequestId} para user=${userId}`);

    res.status(200).json({
        consentRequestId,
        authChannels: ["OTP"],
        authToken: "123456" // OTP fixo para demonstração
    });
});

/**
 * 3. PATCH /consentRequests/{id}
 */
app.patch('/consentRequests/:id', (req, res) => {
    const { id } = req.params;
    const { authToken } = req.body;
    console.log(`[Mock Backend] PATCH /consentRequests/${id} | OTP recebido: ${authToken}`);

    res.status(200).json({
        consentRequestId: id,
        status: "ISSUED",
        scopes: [
            {
                address: "bca.msisdn.2389389274",
                actions: ["ACCOUNTS_GET_BALANCE", "ACCOUNTS_TRANSFER"]
            }
        ]
    });
});

/**
 * 4. POST /consents/{id}/validate
 */
app.post('/consents/:id/validate', (req, res) => {
    console.log(`[Mock Backend] POST /consents/${req.params.id}/validate`);
    res.json({ isValid: true });
});

/**
 * 5. POST /thirdpartyRequests/transactions
 */
app.post('/thirdpartyRequests/transactions', (req, res) => {
    const { transactionRequestId } = req.body;
    console.log(`[Mock Backend] POST /thirdpartyRequests/transactions id=${transactionRequestId}`);
    
    res.json({
        transactionRequestId,
        status: "RECEIVED"
    });
});

app.listen(port, () => {
    console.log(`\n[Mock Backend] Rodando em http://localhost:${port}`);
    console.log(`[Mock Backend] Pronto para ser usado como BACKEND pelo thirdparty-sdk\n`);
});
