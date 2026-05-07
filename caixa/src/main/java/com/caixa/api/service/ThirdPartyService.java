package com.caixa.api.service;

import com.caixa.api.core.model.CoreAccount;
import com.caixa.api.core.repository.CoreAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThirdPartyService {

    @Value("${fsp.id}")
    private String fspId;

    @Value("${fsp.currency}")
    private String fspCurrency;

    @Value("${tp.api.url}")
    private String tpApiUrl;

    private final CoreAccountRepository accountRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Estado em memória para a demo
    private final Map<String, Map<String, Object>> consentRequests = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> consents = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> pendingTransactions = new ConcurrentHashMap<>();
    private final Map<String, String> authorizationToTransaction = new ConcurrentHashMap<>();

    // ─── LINKING ──────────────────────────────────────────────────────────────

    public void sendAccountsCallback(String userId, String fspiDestination) {
        List<Map<String, Object>> accounts = new ArrayList<>();

        accountRepository.findAll().stream()
                .filter(acc -> userId.equals(acc.getMsisdn()))
                .forEach(acc -> {
                    String accountId = fspId + ".msisdn." + acc.getMsisdn();
                    accounts.add(Map.of(
                            "accountNickname", buildNickname(acc),
                            "id", accountId,
                            "currency", acc.getCurrency() != null ? acc.getCurrency() : fspCurrency,
                            "address", accountId
                    ));
                });

        if (accounts.isEmpty()) {
            log.warn("Nenhuma conta encontrada para userId={}", userId);
        }

        CompletableFuture.runAsync(() -> {
            try {
                HttpHeaders headers = buildTpHeaders(fspiDestination);
                Map<String, Object> body = Map.of("accounts", accounts);
                String url = tpApiUrl + "/accounts/" + userId;
                log.info("Callback accounts → PUT {}", url);
                sendJson(HttpMethod.PUT, url, body, headers);
            } catch (Exception e) {
                log.error("Erro no callback accounts: {}", e.getMessage());
            }
        });
    }

    // ─── CONSENT REQUESTS ────────────────────────────────────────────────────

    public void handleConsentRequest(Map<String, Object> body, String fspiSource) {
        String consentRequestId = (String) body.get("consentRequestId");
        consentRequests.put(consentRequestId, new HashMap<>(body));
        log.info("ConsentRequest recebido: id={}", consentRequestId);

        CompletableFuture.runAsync(() -> {
            try {
                HttpHeaders headers = buildTpHeaders(fspiSource);

                // Transformar scopes: accountId/msisdn → address (ex: caixa.msisdn.2389389274)
                List<Map<String, Object>> transformedScopes = new ArrayList<>();
                if (body.containsKey("scopes")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> incomingScopes = (List<Map<String, Object>>) body.get("scopes");
                    for (Map<String, Object> scope : incomingScopes) {
                        String address = (String) scope.get("address");
                        if (address == null) {
                            address = (String) scope.get("accountId");
                        }
                        
                        // Garantir prefixo caixa.msisdn. se for apenas o número
                        if (address != null && !address.contains(".")) {
                            address = fspId + ".msisdn." + address;
                        }

                        Map<String, Object> newScope = new LinkedHashMap<>();
                        newScope.put("address", address);
                        newScope.put("actions", scope.get("actions"));
                        transformedScopes.add(newScope);
                    }
                }

                Map<String, Object> callbackBody = new LinkedHashMap<>();
                callbackBody.put("scopes", transformedScopes);
                callbackBody.put("authChannels", body.get("authChannels"));
                callbackBody.put("callbackUri", body.get("callbackUri"));
                callbackBody.put("authToken", "123456");

                String url = tpApiUrl + "/consentRequests/" + consentRequestId;
                log.info("Callback consentRequests → PUT {} | Source: {} | Dest: {}", url, fspId, fspiSource);
                sendJson(HttpMethod.PUT, url, callbackBody, headers);
            } catch (Exception e) {
                log.error("Erro no callback consentRequests para {}: {}", consentRequestId, e.getMessage());
            }
        });
    }

    public void handleConsentRequestPatch(String consentRequestId, Map<String, Object> body, String fspiSource) {
        Map<String, Object> originalRequest = consentRequests.getOrDefault(consentRequestId, new HashMap<>(body));
        log.info("ConsentRequest PATCH recebido: id={}", consentRequestId);

        CompletableFuture.runAsync(() -> {
            try {
                String consentId = UUID.randomUUID().toString();

                HttpHeaders headers = buildTpHeaders("centralauth");
                Map<String, Object> callbackBody = new HashMap<>();
                callbackBody.put("consentId", consentId);
                callbackBody.put("consentRequestId", consentRequestId);
                callbackBody.put("scopes", originalRequest.get("scopes"));
                callbackBody.put("status", "ISSUED");

                String url = tpApiUrl + "/consents";
                log.info("Criar consent → POST {}", url);
                sendJson(HttpMethod.POST, url, callbackBody, headers);

                Map<String, Object> consentData = new HashMap<>(callbackBody);
                consentData.put("consentRequestId", consentRequestId);
                consents.put(consentId, consentData);

            } catch (Exception e) {
                log.error("Erro ao criar consent: {}", e.getMessage());
            }
        });
    }

    // ─── CONSENTS ────────────────────────────────────────────────────────────

    public void handleConsent(Map<String, Object> body, String fspiSource) {
        String consentId = (String) body.get("consentId");
        consents.put(consentId, new HashMap<>(body));
        log.info("Consent recebido: id={}", consentId);

        CompletableFuture.runAsync(() -> {
            try {
                HttpHeaders headers = buildTpHeaders("centralauth");

                @SuppressWarnings("unchecked")
                Map<String, Object> incomingCredential = body.containsKey("credential")
                        ? new HashMap<>((Map<String, Object>) body.get("credential"))
                        : new HashMap<>();
                incomingCredential.put("status", "VERIFIED");

                Map<String, Object> callbackBody = new HashMap<>();
                callbackBody.put("credential", incomingCredential);
                callbackBody.put("status", "ACTIVE");

                String url = tpApiUrl + "/consents/" + consentId;
                log.info("Confirmar consent → PATCH {}", url);
                sendJson(HttpMethod.PATCH, url, callbackBody, headers);
            } catch (Exception e) {
                log.error("Erro ao confirmar consent: {}", e.getMessage());
            }
        });
    }

    // ─── TRANSFER ────────────────────────────────────────────────────────────

    public void handleTransactionRequest(Map<String, Object> body, String fspiSource) {
        String transactionRequestId = (String) body.get("transactionRequestId");
        String transactionId = UUID.randomUUID().toString();
        String authorizationRequestId = UUID.randomUUID().toString();

        Map<String, Object> txData = new HashMap<>(body);
        txData.put("_transactionId", transactionId);
        txData.put("_authorizationRequestId", authorizationRequestId);
        txData.put("_fspiSource", fspiSource);
        pendingTransactions.put(transactionRequestId, txData);
        authorizationToTransaction.put(authorizationRequestId, transactionRequestId);
        log.info("TransactionRequest recebido: id={}, authorizationId={}", transactionRequestId, authorizationRequestId);

        CompletableFuture.runAsync(() -> {
            try {
                HttpHeaders headers = buildTpHeaders(fspiSource);

                // Callback 1: confirmar recepção
                Map<String, Object> receivedCallback = new HashMap<>();
                receivedCallback.put("transactionId", transactionId);
                receivedCallback.put("transactionRequestState", "RECEIVED");
                String url1 = tpApiUrl + "/thirdpartyRequests/transactions/" + transactionRequestId;
                log.info("Callback RECEIVED → PUT {}", url1);
                sendJson(HttpMethod.PUT, url1, receivedCallback, headers);

                Thread.sleep(300);

                // Callback 2: pedir autorização ao PISP
                @SuppressWarnings("unchecked")
                Map<String, Object> amountMap = body.containsKey("amount")
                        ? (Map<String, Object>) body.get("amount")
                        : Map.of("currency", fspCurrency, "amount", "0");
                String currency = (String) amountMap.getOrDefault("currency", fspCurrency);
                String amount = (String) amountMap.getOrDefault("amount", "0");

                String challenge = buildChallenge(transactionRequestId + ":" + transactionId);
                Map<String, Object> moneyAmount = Map.of("currency", currency, "amount", amount);

                Map<String, Object> authRequest = new LinkedHashMap<>();
                authRequest.put("authorizationRequestId", authorizationRequestId);
                authRequest.put("transactionRequestId", transactionRequestId);
                authRequest.put("challenge", challenge);
                authRequest.put("transferAmount", moneyAmount);
                authRequest.put("payeeReceiveAmount", moneyAmount);
                authRequest.put("fees", Map.of("currency", currency, "amount", "0"));
                authRequest.put("payer", body.get("payer"));
                authRequest.put("payee", body.get("payee"));
                authRequest.put("transactionType", body.get("transactionType"));
                authRequest.put("expiration", body.get("expiration"));

                String url2 = tpApiUrl + "/thirdpartyRequests/authorizations";
                log.info("Pedido de autorização → POST {}", url2);
                sendJson(HttpMethod.POST, url2, authRequest, headers);

            } catch (Exception e) {
                log.error("Erro ao processar transactionRequest: {}", e.getMessage());
            }
        });
    }

    public void handleAuthorization(String authorizationRequestId, Map<String, Object> body, String fspiSource) {
        String transactionRequestId = authorizationToTransaction.get(authorizationRequestId);
        log.info("Authorization recebida: id={}, transactionRequestId={}", authorizationRequestId, transactionRequestId);

        if (transactionRequestId == null) {
            log.warn("Authorization sem transactionRequest correspondente: {}", authorizationRequestId);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Para demo: aceitar sempre — em produção verificar assinatura FIDO
                HttpHeaders headers = buildTpHeaders(fspiSource);
                Map<String, Object> patchBody = Map.of("transactionRequestState", "ACCEPTED");
                String url = tpApiUrl + "/thirdpartyRequests/transactions/" + transactionRequestId;
                log.info("Transfer ACCEPTED → PATCH {}", url);
                sendJson(HttpMethod.PATCH, url, patchBody, headers);
            } catch (Exception e) {
                log.error("Erro ao processar authorization: {}", e.getMessage());
            }
        });
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private void sendJson(HttpMethod method, String url, Object body, HttpHeaders headers) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        restTemplate.exchange(url, method, entity, String.class);
    }

    private HttpHeaders buildTpHeaders(String fspiDestination) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/vnd.interoperability.thirdparty+json;version=1.0");
        headers.set("Accept", "application/vnd.interoperability.thirdparty+json;version=1.0");
        headers.set("FSPIOP-Source", fspId);
        headers.set("FSPIOP-Destination", fspiDestination != null ? fspiDestination : "hub");
        headers.set("Date", utcDate());
        return headers;
    }

    private String utcDate() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH));
    }

    private String buildChallenge(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(input.getBytes());
        }
    }

    private String buildNickname(CoreAccount acc) {
        if (acc.getDisplayName() != null && !acc.getDisplayName().isBlank()) {
            return acc.getDisplayName();
        }
        String name = ((acc.getFirstName() != null ? acc.getFirstName() : "") + " "
                + (acc.getLastName() != null ? acc.getLastName() : "")).trim();
        return name.isBlank() ? "Conta " + acc.getAccountNumber() : "Conta Corrente " + name;
    }
}
