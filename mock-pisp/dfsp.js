/**
 * Mock DFSP (Banco BCA/Caixa) — Simulador Node.js
 *
 * Substitui a API Java para testes rápidos do fluxo Third Party API.
 * Implementa os mesmos endpoints que o Java, com callbacks correctos.
 *
 * USAGE:
 *   node dfsp.js
 *   PORT=8081 FSP_ID=bca node dfsp.js
 *   PORT=8082 FSP_ID=caixa node dfsp.js
 *
 * O Hub vai chamar este simulador em vez da API Java.
 */

const express = require('express');
const axios   = require('axios');
const crypto  = require('crypto');

const app = express();
app.use(express.json({ type: '*/*' }));

// ── CONFIGURAÇÃO ───────────────────────────────────────────────
const PORT       = process.env.PORT       || 8081;
const FSP_ID     = process.env.FSP_ID     || 'bca';
const CURRENCY   = process.env.CURRENCY   || 'CVE';
const HUB_TP_URL = process.env.HUB_TP_URL || 'http://tp-api-svc.kretxeucv.cv';

// Contas de teste (mesmas que o DatabaseSeeder do Java)
const ACCOUNTS = {
  bca: [
    { msisdn: '2389512347', firstName: 'Vicente', lastName: 'Andrade', balance: 50000 },
    { msisdn: '2389389274', firstName: 'Maria',   lastName: 'Santos',  balance: 30000 },
  ],
  caixa: [
    { msisdn: '2389634521', firstName: 'Alvaro',  lastName: 'Silva',   balance: 50000 },
    { msisdn: '2389389275', firstName: 'Carlos',  lastName: 'Fonseca', balance: 30000 },
  ],
};

const accounts = ACCOUNTS[FSP_ID] || ACCOUNTS.bca;

// Estado em memória
const consentRequests = {};
const consents        = {};
const transactions    = {};
const authorizations  = {};

// ── LOGGING ────────────────────────────────────────────────────
const C = {
  reset: '\x1b[0m', bold: '\x1b[1m',
  green: '\x1b[32m', yellow: '\x1b[33m',
  blue: '\x1b[34m', cyan: '\x1b[36m', red: '\x1b[31m',
};

function logIn(method, path, body) {
  console.log(`\n${C.bold}${'─'.repeat(60)}${C.reset}`);
  console.log(`${C.green}◄ HUB  ${C.reset}${C.bold}${method} ${path}${C.reset}`);
  if (body && Object.keys(body).length) console.log(JSON.stringify(body, null, 2));
}

function logOut(method, path) {
  console.log(`${C.cyan}► BANCO ${C.reset}${method} ${HUB_TP_URL}${path}`);
}

// ── CALLBACK HELPER ────────────────────────────────────────────
async function callback(method, path, body, destination) {
  const url = `${HUB_TP_URL}${path}`;
  logOut(method, path);
  try {
    const res = await axios({
      method, url, data: body,
      headers: {
        'Content-Type':       'application/vnd.interoperability.thirdparty+json;version=1.0',
        'Accept':             'application/vnd.interoperability.thirdparty+json;version=1.0',
        'FSPIOP-Source':      FSP_ID,
        'FSPIOP-Destination': destination || 'hub',
        'Date':               new Date().toUTCString(),
      },
      validateStatus: () => true,
    });
    console.log(`   ${res.status < 300 ? C.green : C.red}HTTP ${res.status}${C.reset}`);
  } catch (err) {
    console.error(`   ${C.red}ERRO: ${err.message}${C.reset}`);
  }
}

function after(ms, fn) { setTimeout(fn, ms); }

// ── HELPERS ────────────────────────────────────────────────────
function findAccount(msisdn) {
  return accounts.find(a => a.msisdn === msisdn);
}

function buildAccountId(msisdn) {
  return `${FSP_ID}.msisdn.${msisdn}`;
}

function transformScopes(scopes = []) {
  return scopes.map(s => ({
    address: s.address || s.accountId,
    actions: s.actions || ['ACCOUNTS_GET_BALANCE', 'ACCOUNTS_TRANSFER'],
  }));
}

// ── ENDPOINTS ─────────────────────────────────────────────────

/**
 * GET /accounts/{userId}
 *
 * Com FSPIOP-Source → fluxo TP (202 + callback)
 * Sem FSPIOP-Source → resposta directa (uso interno/app)
 */
app.get('/accounts/:userId', async (req, res) => {
  const { userId } = req.params;
  const fspiSource = req.headers['fspiop-source'];
  logIn('GET', `/accounts/${userId}`, {});

  const acc = findAccount(userId);

  // Chamada interna sem header TP — resposta directa
  if (!fspiSource) {
    if (!acc) return res.status(404).json({ error: 'Conta não encontrada' });
    return res.json({
      msisdn: acc.msisdn, firstName: acc.firstName,
      lastName: acc.lastName, balance: acc.balance, currency: CURRENCY,
    });
  }

  // Chamada do Hub — 202 + callback
  res.status(202).send();

  const accountId = buildAccountId(userId);
  const accountList = acc
    ? [{ accountNickname: `Conta Corrente ${acc.firstName} ${acc.lastName}`, id: accountId, currency: CURRENCY, address: accountId }]
    : [];

  after(300, () => callback('PUT', `/accounts/${userId}`, { accounts: accountList }, fspiSource));
});

/**
 * POST /consentRequests
 *
 * Hub/PISP quer vincular conta. Responder com OTP (fixo para demo).
 */
app.post('/consentRequests', (req, res) => {
  const { consentRequestId, userId, scopes, authChannels, callbackUri } = req.body;
  const fspiSource = req.headers['fspiop-source'] || 'hub';
  logIn('POST', '/consentRequests', req.body);

  consentRequests[consentRequestId] = { consentRequestId, userId, scopes, authChannels, callbackUri };
  res.status(202).send();

  // thirdparty-sdk v15.1.3 injeta sempre ["OTP","WEB"]; tp-api-svc v15.1.5 exige
  // authUri quando WEB está presente e não aceita authToken no canal WEB.
  after(400, () => callback('PUT', `/consentRequests/${consentRequestId}`, {
    consentRequestId,
    scopes:       transformScopes(scopes),
    authChannels: ['WEB'],
    callbackUri:  callbackUri || '',
    authUri:      `http://${FSP_ID}.kretxeucv.cv/authorize?consentRequestId=${consentRequestId}`,
  }, fspiSource));
});

/**
 * PATCH /consentRequests/{ID}
 *
 * PISP confirma o OTP. Para demo: aceitar sempre qualquer token.
 * Criar consent e notificar centralauth.
 */
app.patch('/consentRequests/:id', (req, res) => {
  const { id } = req.params;
  const fspiSource = req.headers['fspiop-source'] || 'hub';
  logIn('PATCH', `/consentRequests/${id}`, req.body);

  const original = consentRequests[id] || {};
  const consentId = crypto.randomUUID();

  consents[consentId] = { consentId, consentRequestId: id, scopes: transformScopes(original.scopes) };
  res.status(202).send();

  after(400, () => callback('POST', '/consents', {
    consentId,
    consentRequestId: id,
    scopes:  transformScopes(original.scopes),
    status:  'ISSUED',
  }, 'centralauth'));
});

/**
 * POST /consents
 *
 * Hub envia credencial FIDO para registar. Para demo: aceitar sempre.
 * Confirmar com VERIFIED via PATCH.
 */
app.post('/consents', (req, res) => {
  const { consentId, credential } = req.body;
  logIn('POST', '/consents', req.body);

  if (consentId) consents[consentId] = { ...consents[consentId], ...req.body, status: 'ACTIVE' };
  res.status(202).send();

  // Devolver credencial com status VERIFIED
  const verifiedCredential = credential
    ? { ...credential, status: 'VERIFIED' }
    : { credentialType: 'FIDO', status: 'VERIFIED', payload: req.body.credential?.payload };

  after(400, () => callback('PATCH', `/consents/${consentId}`, {
    credential: verifiedCredential,
    status:     'ACTIVE',
  }, 'centralauth'));
});

/**
 * POST /thirdpartyRequests/transactions
 *
 * PISP quer iniciar pagamento.
 * 1. Confirmar recepção (RECEIVED)
 * 2. Pedir autorização ao PISP (challenge FIDO)
 */
app.post('/thirdpartyRequests/transactions', async (req, res) => {
  const body = req.body;
  const { transactionRequestId, amount } = body;
  const fspiSource = req.headers['fspiop-source'] || 'hub';
  logIn('POST', '/thirdpartyRequests/transactions', body);

  const transactionId          = crypto.randomUUID();
  const authorizationRequestId = crypto.randomUUID();
  const challenge              = crypto.createHash('sha256')
    .update(`${transactionRequestId}:${transactionId}`)
    .digest('base64');

  transactions[transactionRequestId]     = { ...body, transactionId, authorizationRequestId };
  authorizations[authorizationRequestId] = transactionRequestId;
  res.status(202).send();

  // Callback 1 — RECEIVED
  after(300, async () => {
    await callback('PUT', `/thirdpartyRequests/transactions/${transactionRequestId}`, {
      transactionId,
      transactionRequestState: 'RECEIVED',
    }, fspiSource);

    // Callback 2 — Pedido de autorização
    after(300, () => {
      const amt = amount || { currency: CURRENCY, amount: '0' };
      callback('POST', '/thirdpartyRequests/authorizations', {
        authorizationRequestId,
        transactionRequestId,
        challenge,
        transferAmount:      amt,
        payeeReceiveAmount:  amt,
        fees:                { currency: amt.currency, amount: '0' },
        payer:               body.payer,
        payee:               body.payee,
        transactionType:     body.transactionType,
        expiration:          body.expiration,
      }, fspiSource);
    });
  });
});

/**
 * PUT /thirdpartyRequests/authorizations/{ID}
 *
 * PISP responde com assinatura FIDO. Para demo: aceitar sempre.
 * Notificar que o transfer foi ACCEPTED.
 */
app.put('/thirdpartyRequests/authorizations/:id', (req, res) => {
  const { id } = req.params;
  const fspiSource = req.headers['fspiop-source'] || 'hub';
  logIn('PUT', `/thirdpartyRequests/authorizations/${id}`, req.body);

  const transactionRequestId = authorizations[id];
  res.status(202).send();

  after(400, () => callback('PATCH', `/thirdpartyRequests/transactions/${transactionRequestId}`, {
    transactionRequestState: 'ACCEPTED',
  }, fspiSource));
});

// ── HEALTH ─────────────────────────────────────────────────────
app.get('/health', (_, res) => res.json({ status: 'OK', fsp: FSP_ID, port: PORT }));

// ── START ──────────────────────────────────────────────────────
app.listen(PORT, () => {
  console.log(`\n${'═'.repeat(60)}`);
  console.log(`${C.bold}  🏦 Mock DFSP — ${FSP_ID.toUpperCase()}${C.reset}`);
  console.log(`${'═'.repeat(60)}`);
  console.log(`  Porta    : ${PORT}`);
  console.log(`  FSP ID   : ${FSP_ID}`);
  console.log(`  Moeda    : ${CURRENCY}`);
  console.log(`  Hub URL  : ${HUB_TP_URL}`);
  console.log(`${'─'.repeat(60)}`);
  console.log(`  Contas de teste:`);
  accounts.forEach(a => console.log(`    ${a.msisdn} — ${a.firstName} ${a.lastName} (${a.balance} ${CURRENCY})`));
  console.log(`${'═'.repeat(60)}\n`);
});
