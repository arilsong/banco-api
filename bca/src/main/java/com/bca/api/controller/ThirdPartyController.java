package com.bca.api.controller;

import com.bca.api.service.ThirdPartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ThirdPartyController {

    private final ThirdPartyService thirdPartyService;

    // ─── LINKING ──────────────────────────────────────────────────────────────

    /**
     * GET /accounts/{userId} — versão Third Party API.
     * Só activa quando o Hub envia o header FSPIOP-Source.
     * O endpoint original em AccountsController continua a funcionar
     * para chamadas internas sem esse header.
     */
    @GetMapping(value = "/accounts/{userId}", headers = "FSPIOP-Source")
    public ResponseEntity<Void> getAccounts(
            @PathVariable String userId,
            @RequestHeader("FSPIOP-Source") String fspiSource) {
        log.info("GET /accounts/{} — Hub FSPIOP-Source={}", userId, fspiSource);
        thirdPartyService.sendAccountsCallback(userId, fspiSource);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/consentRequests")
    public ResponseEntity<Void> postConsentRequest(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "FSPIOP-Source", defaultValue = "hub") String fspiSource) {
        log.info("POST /consentRequests id={} de {}", body.get("consentRequestId"), fspiSource);
        thirdPartyService.handleConsentRequest(body, fspiSource);
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/consentRequests/{id}")
    public ResponseEntity<Void> patchConsentRequest(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "FSPIOP-Source", defaultValue = "hub") String fspiSource) {
        log.info("PATCH /consentRequests/{} de {}", id, fspiSource);
        thirdPartyService.handleConsentRequestPatch(id, body, fspiSource);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/consents")
    public ResponseEntity<Void> postConsent(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "FSPIOP-Source", defaultValue = "hub") String fspiSource) {
        log.info("POST /consents id={} de {}", body.get("consentId"), fspiSource);
        thirdPartyService.handleConsent(body, fspiSource);
        return ResponseEntity.accepted().build();
    }

    // ─── TRANSFER ────────────────────────────────────────────────────────────

    @PostMapping("/thirdpartyRequests/transactions")
    public ResponseEntity<Void> postTransactionRequest(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "FSPIOP-Source", defaultValue = "hub") String fspiSource) {
        log.info("POST /thirdpartyRequests/transactions id={} de {}", body.get("transactionRequestId"), fspiSource);
        thirdPartyService.handleTransactionRequest(body, fspiSource);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/thirdpartyRequests/authorizations/{id}")
    public ResponseEntity<Void> putAuthorization(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "FSPIOP-Source", defaultValue = "hub") String fspiSource) {
        log.info("PUT /thirdpartyRequests/authorizations/{} de {}", id, fspiSource);
        thirdPartyService.handleAuthorization(id, body, fspiSource);
        return ResponseEntity.accepted().build();
    }
}
