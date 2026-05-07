package com.bca.api.service;

import com.bca.api.core.model.CoreAccount;
import com.bca.api.core.repository.CoreAccountRepository;
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

    /**
     * GET /accounts/{userId} — Retorna a lista de contas diretamente (Simples).
     */
    public Map<String, Object> getAccountsSimple(String userId) {
        log.info("Buscando contas (Simples) para userId: {}", userId);
        
        List<Map<String, Object>> userAccounts = new ArrayList<>();
        accountRepository.findAll().stream()
                .filter(acc -> userId.equals(acc.getMsisdn()))
                .forEach(acc -> {
                    String accountId = fspId + ".msisdn." + acc.getMsisdn();
                    userAccounts.add(Map.of(
                            "accountNickname", buildNickname(acc),
                            "id", accountId,
                            "currency", acc.getCurrency() != null ? acc.getCurrency() : fspCurrency,
                            "address", accountId
                    ));
                });

        return Map.of("accounts", userAccounts);
    }

    /**
     * POST /consentRequests — Responde síncronamente ao SDK (Simples).
     */
    public Map<String, Object> handleConsentRequestSimple(Map<String, Object> body) {
        String consentRequestId = (String) body.get("consentRequestId");
        log.info("Processando pedido de consentimento (Simples) id={}", consentRequestId);
        
        consentRequests.put(consentRequestId, new HashMap<>(body));

        return Map.of(
            "consentRequestId", consentRequestId,
            "authToken", "123456" // OTP fixo para testes
        );
    }

    /**
     * PATCH /consentRequests/{id} — PISP confirmou o OTP (Simples).
     */
    public Map<String, Object> handleConsentRequestPatchSimple(String id, Map<String, Object> body) {
        log.info("PATCH /consentRequests/{} — OTP Confirmado: {}", id, body.get("authToken"));

        Map<String, Object> originalRequest = consentRequests.get(id);
        
        return Map.of(
            "consentRequestId", id,
            "status", "ISSUED",
            "scopes", originalRequest != null ? originalRequest.get("scopes") : new ArrayList<>()
        );
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

                // Guardar consent criado para uso posterior
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
    /**
     * POST /consents/{id}/validate — Validação FIDO (Simples).
     */
    public Map<String, Object> handleFidoValidate(String id, Map<String, Object> body) {
        log.info("Validando credencial FIDO para consent {} | body: {}", id, body);
        
        return Map.of("isValid", true);
    }

    /**
     * POST /thirdpartyRequests/transactions — Iniciar pagamento (Simples).
     */
    public Map<String, Object> handleTransactionRequestSimple(Map<String, Object> body) {
        String transactionRequestId = (String) body.get("transactionRequestId");
        log.info("Iniciando transação Third Party (Simples): {}", transactionRequestId);

        // Armazenar para referência
        pendingTransactions.put(transactionRequestId, new HashMap<>(body));

        return Map.of(
            "transactionRequestId", transactionRequestId,
            "status", "RECEIVED"
        );
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private String buildNickname(CoreAccount acc) {
        if (acc.getDisplayName() != null && !acc.getDisplayName().isBlank()) {
            return acc.getDisplayName();
        }
        String name = ((acc.getFirstName() != null ? acc.getFirstName() : "") + " "
                + (acc.getLastName() != null ? acc.getLastName() : "")).trim();
        return name.isBlank() ? "Conta " + acc.getAccountNumber() : "Conta Corrente " + name;
    }
}
