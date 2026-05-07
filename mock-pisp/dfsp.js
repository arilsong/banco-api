const express = require('express');
const axios = require('axios');
const app = express();
const port = process.env.PORT || 8081; 

app.use(express.json());

const fspId = 'bca';
const tpApiUrl = process.env.HUB_URL || 'http://localhost:4015'; 

// Simulação de base de dados em memória
const consentRequests = {};
const accounts = [
    {
        id: "bca.msisdn.2389389274",
        currency: "CVE",
        nickname: "Conta Corrente Arilson",
        userId: "2389389274"
    },
    {
        id: "bca.msisdn.999112233",
        currency: "CVE",
        nickname: "Conta Poupança Teste",
        userId: "999112233"
    }
];

console.log(`[DFSP Node] Iniciando simulador BCA na porta ${port}...`);

// ─── LINKING ENDPOINTS ──────────────────────────────────────────────────────

// GET /accounts/{userId} - Chamado pelo Switch para listar contas do utilizador
app.get('/accounts/:userId', (req, res) => {
    const userId = req.params.userId;
    const fspiSource = req.headers['fspiop-source'] || 'meu-pisp';
    
    console.log(`[DFSP Node] Recebido GET /accounts/${userId} de ${fspiSource}`);

    const userAccounts = accounts.filter(acc => acc.userId === userId).map(acc => ({
        accountNickname: acc.nickname,
        id: acc.id,
        currency: acc.currency,
        address: acc.id
    }));

    // Enviar callback PUT /accounts/{userId}
    setTimeout(async () => {
        try {
            const url = `${tpApiUrl}/accounts/${userId}`;
            console.log(`[DFSP Node] Enviando Callback de Contas PUT ${url}`);

            await axios.put(url, { accounts: userAccounts }, {
                headers: {
                    'Content-Type': 'application/vnd.interoperability.thirdparty+json;version=1.0',
                    'FSPIOP-Source': fspId,
                    'FSPIOP-Destination': fspiSource,
                    'Date': new Date().toUTCString()
                }
            });
            console.log(`[DFSP Node] Callback de contas enviado!`);
        } catch (error) {
            console.error(`[DFSP Node] Erro no callback de contas:`, error.message);
        }
    }, 500);

    res.status(202).send();
});

// ─── ENDPOINTS MOJALOOP INBOUND (O que o Switch chama) ──────────────────────

// POST /consentRequests
app.post('/consentRequests', (req, res) => {
    const { consentRequestId, userId, authChannels, callbackUri, scopes } = req.body;
    const fspiSource = req.headers['fspiop-source'] || 'meu-pisp';
    
    console.log(`[DFSP Node] Recebido POST /consentRequests: ${consentRequestId}`);
    
    if (!scopes || !Array.isArray(scopes)) {
        console.error("[DFSP Node] Erro: scopes ausente ou inválido");
        return res.status(400).json({ error: 'scopes obrigatório e deve ser um array' });
    }

    consentRequests[consentRequestId] = req.body;

    // Simular processamento assíncrono
    setTimeout(async () => {
        try {
            const authUri = `${callbackUri}/linking/request-consent/${consentRequestId}/authenticate`;

            // Transformar scopes para o formato que o Switch espera (v2.0)
            const scopesOut = scopes.map(s => ({
                accountId: s.address || s.accountId || `${fspId}.msisdn.${userId}`,
                actions: s.actions
            }));

            const callbackBody = {
                consentRequestId,
                authChannels,
                authUri,
                callbackUri,
                scopes: scopesOut
            };

            const url = `${tpApiUrl}/consentRequests/${consentRequestId}`;
            console.log(`[DFSP Node] Enviando PUT Callback para ${url}`);

            await axios.put(url, callbackBody, {
                headers: {
                    'Content-Type': 'application/vnd.interoperability.consentRequests+json;version=2.0',
                    'Accept': 'application/vnd.interoperability.consentRequests+json;version=2.0',
                    'FSPIOP-Source': fspId,
                    'FSPIOP-Destination': fspiSource,
                    'Date': new Date().toUTCString()
                }
            });
            console.log(`[DFSP Node] PUT Callback enviado com sucesso!`);
            
            // Simular envio de SMS OTP
            console.log(`[SMS] Enviando OTP para o usuário ${userId}...`);
            
        } catch (error) {
            console.error(`[DFSP Node] Erro no callback:`, error.response?.data || error.message);
        }
    }, 500);

    res.status(202).send();
});

// PATCH /consentRequests/{id} (Aprovação do PISP)
app.patch('/consentRequests/:id', (req, res) => {
    const id = req.params.id;
    console.log(`[DFSP Node] Recebido PATCH /consentRequests/${id}`);
    
    // Simular criação de Consentimento
    setTimeout(async () => {
        const consentId = `con-${Math.floor(Math.random() * 100000)}`;
        const originalRequest = consentRequests[id];

        const callbackBody = {
            consentId: consentId,
            consentRequestId: id,
            scopes: originalRequest?.scopes || [],
            status: "ISSUED"
        };

        const url = `${tpApiUrl}/consents`;
        console.log(`[DFSP Node] Criando Consentimento POST ${url}`);

        await axios.post(url, callbackBody, {
            headers: {
                'Content-Type': 'application/vnd.interoperability.thirdparty+json;version=1.0',
                'FSPIOP-Source': fspId,
                'FSPIOP-Destination': 'centralauth',
                'Date': new Date().toUTCString()
            }
        });
    }, 500);

    res.status(202).send();
});

app.listen(port, () => {
    console.log(`[DFSP Node] BCA Simulator rodando em http://localhost:${port}`);
});
