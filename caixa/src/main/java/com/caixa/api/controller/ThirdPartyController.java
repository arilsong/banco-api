package com.caixa.api.controller;

import com.caixa.api.service.ThirdPartyService;
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
     * Só activa quando o Hub envia o header FSPIOP-Source (pedido Mojaloop).
     * O endpoint original em AccountsController continua a funcionar para
     * chamadas internas sem esse header.
     */
    @GetMapping(value = "/accounts/{userId}", headers = "FSPIOP-Source")
    public ResponseEntity<Map<String, Object>> getAccounts(
            @PathVariable String userId) {
        log.info("GET /accounts/{} (Simples)", userId);
        return ResponseEntity.ok(thirdPartyService.getAccountsSimple(userId));
    }

    @PostMapping("/consentRequests")
    public ResponseEntity<Map<String, Object>> postConsentRequest(
            @RequestBody Map<String, Object> body) {
        log.info("POST /consentRequests id={} (Simples)", body.get("consentRequestId"));
        return ResponseEntity.ok(thirdPartyService.handleConsentRequestSimple(body));
    }

    @PatchMapping("/consentRequests/{id}")
    public ResponseEntity<Map<String, Object>> patchConsentRequest(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        log.info("PATCH /consentRequests/{} (Simples)", id);
        return ResponseEntity.ok(thirdPartyService.handleConsentRequestPatchSimple(id, body));
    }

    @PostMapping("/consents/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateConsent(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        log.info("POST /consents/{}/validate (Simples)", id);
        return ResponseEntity.ok(thirdPartyService.handleFidoValidate(id, body));
    }

    // ─── TRANSFER ────────────────────────────────────────────────────────────

    @PostMapping("/thirdpartyRequests/transactions")
    public ResponseEntity<Map<String, Object>> postTransactionRequest(
            @RequestBody Map<String, Object> body) {
        log.info("POST /thirdpartyRequests/transactions (Simples)");
        return ResponseEntity.ok(thirdPartyService.handleTransactionRequestSimple(body));
    }
}
