// Adicionar este ficheiro: mock-dfsp-backend.js
const express = require('express');
const axios = require('axios');
const app = express();
app.use(express.json({ type: '*/*' }));

const HUB_TP_API = 'http://tp-api-svc.kretxeucv.cv';
const DFSP_ID = 'bca';

const hubCallback = async (method, path, body, destination) => {
  const url = `${HUB_TP_API}${path}`;
  console.log(`[CALLBACK] ${method} ${url}`);
  try {
    const res = await axios({ method, url, data: body, headers: {
      'Content-Type': 'application/vnd.interoperability.thirdparty+json;version=1.0',
      'FSPIOP-Source': DFSP_ID,
      'FSPIOP-Destination': destination || 'Hub',
      'Date': new Date().toUTCString()
    }});
    console.log(`[CALLBACK OK] ${res.status}`);
  } catch (err) {
    console.error(`[CALLBACK ERR] ${err.response?.status}`, JSON.stringify(err.response?.data));
  }
};

// GET /accounts/{userId} — já funciona, só precisa de fazer callback
app.get('/accounts/:userId', async (req, res) => {
  const { userId } = req.params;
  const source = req.headers['fspiop-source'];
  console.log(`[GET /accounts/${userId}] de ${source}`);
  res.status(202).send();

  setTimeout(async () => {
    await hubCallback('PUT', `/accounts/${userId}`, {
      accounts: [{
        accountNickname: userId === '2389389274' ? 'Maria Santos' : 'Vicente Andrade',
        id: `bca.msisdn.${userId}`,
        currency: 'CVE',
        address: `bca.msisdn.${userId}`
      }]
    }, source);
  }, 200);
});

// POST /consentRequests — recebe e faz callback com OTP fixo
app.post('/consentRequests', async (req, res) => {
  const { consentRequestId, userId, scopes, authChannels, callbackUri } = req.body;
  const source = req.headers['fspiop-source'];
  console.log(`[POST /consentRequests] id=${consentRequestId} de ${source}`);
  res.status(202).send();

  setTimeout(async () => {
    await hubCallback('PUT', `/consentRequests/${consentRequestId}`, {
      consentRequestId,
      scopes,
      authChannels: ['OTP'],
      callbackUri,
      authToken: '123456'
    }, source);
  }, 500);
});

// PATCH /consentRequests/{ID} — confirma OTP e cria consent
app.patch('/consentRequests/:id', async (req, res) => {
  const { id } = req.params;
  const { scopes } = req.body;
  const source = req.headers['fspiop-source'];
  console.log(`[PATCH /consentRequests/${id}] OTP confirmado de ${source}`);
  res.status(202).send();

  const consentId = require('crypto').randomUUID();
  setTimeout(async () => {
    await hubCallback('POST', '/consents', {
      consentId,
      consentRequestId: id,
      scopes: scopes || [],
      status: 'ISSUED'
    }, source);
  }, 500);
});

// POST /consents/{ID} — valida credencial FIDO (aceita sempre)
app.post('/consents/:id', async (req, res) => {
  const { id } = req.params;
  const source = req.headers['fspiop-source'];
  console.log(`[POST /consents/${id}] credencial recebida de ${source}`);
  res.status(202).send();

  setTimeout(async () => {
    await hubCallback('PATCH', `/consents/${id}`, {
      credential: {
        credentialType: 'FIDO',
        status: 'VERIFIED',
        payload: {
          id: 'HskU2gw4np09IUtYNHnxMM696jJHqvccUdBmd0xP6XEWwH0xLei1PUzDJCM19SZ3A2Ex0fNLw0nc2hrIlFnAtw',
          rawId: 'HskU2gw4np09IUtYNHnxMM696jJHqvccUdBmd0xP6XEWwH0xLei1PUzDJCM19SZ3A2Ex0fNLw0nc2hrIlFnAtw==',
          response: { clientDataJSON: 'e30=', attestationObject: 'e30=' },
          type: 'public-key'
        }
      },
      status: 'ACTIVE'
    }, 'centralauth');
  }, 500);
});

app.listen(8081, () => {
  console.log('[Mock Backend] Rodando em http://localhost:8081');
  console.log('[Mock Backend] Pronto para receber chamadas do Hub');
});