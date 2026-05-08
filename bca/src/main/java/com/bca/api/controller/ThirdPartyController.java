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
     * GET /accounts/{userId}
     * Retorna a lista de contas síncronamente para o SDK.
     */
    @GetMapping("/accounts/{userId}")
    public ResponseEntity<Map<String, Object>> getAccounts(@PathVariable String userId) {
        log.info("GET /accounts/{} — Chamada síncrona do SDK", userId);
        return ResponseEntity.ok(thirdPartyService.getAccountsSync(userId));
    }

    @PostMapping("/consentRequests")
    public ResponseEntity<Map<String, Object>> postConsentRequest(@RequestBody Map<String, Object> body) {
        log.info("POST /consentRequests id={}", body.get("consentRequestId"));
        return ResponseEntity.ok(thirdPartyService.handleConsentRequestSync(body));
    }

    @PatchMapping("/consentRequests/{id}")
    public ResponseEntity<Map<String, Object>> patchConsentRequest(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        log.info("PATCH /consentRequests/{} (OTP Validation)", id);
        return ResponseEntity.ok(thirdPartyService.handleConsentRequestPatchSync(id, body));
    }

    @PostMapping("/consents/{id}/validate")
    public ResponseEntity<Map<String, Object>> validateConsent(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        log.info("POST /consents/{}/validate (FIDO Validation)", id);
        return ResponseEntity.ok(Map.of("isValid", true));
    }

    // ─── TRANSFER ────────────────────────────────────────────────────────────

    @PostMapping("/thirdpartyRequests/transactions")
    public ResponseEntity<Map<String, Object>> postTransactionRequest(@RequestBody Map<String, Object> body) {
        log.info("POST /thirdpartyRequests/transactions id={}", body.get("transactionRequestId"));
        return ResponseEntity.ok(Map.of(
                "transactionRequestId", body.get("transactionRequestId"),
                "transactionRequestState", "RECEIVED"
        ));
    }

    @PutMapping("/thirdpartyRequests/authorizations/{id}")
    public ResponseEntity<Map<String, Object>> putAuthorization(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        log.info("PUT /thirdpartyRequests/authorizations/{} (FIDO Signature)", id);
        return ResponseEntity.ok(Map.of("authorizationState", "ACCEPTED"));
    }
}
