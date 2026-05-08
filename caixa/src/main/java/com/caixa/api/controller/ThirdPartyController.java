package com.caixa.api.controller;

import com.caixa.api.service.ThirdPartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ThirdPartyController {

    private final ThirdPartyService thirdPartyService;

    // ─── LINKING ──────────────────────────────────────────────────────────────

    /**
     * GET /accounts/{ID}
     */
    @GetMapping("/accounts/{ID}")
    public ResponseEntity<Map<String, Object>> getAccounts(@PathVariable("ID") String userId) {
        log.info("GET /accounts/{} — Chamada síncrona do SDK", userId);
        return ResponseEntity.ok(thirdPartyService.getAccountsSync(userId));
    }

    /**
     * POST /validateConsentRequests
     */
    @PostMapping("/validateConsentRequests")
    public ResponseEntity<Map<String, Object>> validateConsentRequest(@RequestBody Map<String, Object> body) {
        log.info("POST /validateConsentRequests id={}", body.get("consentRequestId"));
        return ResponseEntity.ok(thirdPartyService.validateConsentRequestSync(body));
    }

    /**
     * POST /store/consentRequests/{ID}
     */
    @PostMapping("/store/consentRequests/{ID}")
    public ResponseEntity<Void> storeConsentRequest(
            @PathVariable("ID") String id,
            @RequestBody Map<String, Object> body) {
        log.info("POST /store/consentRequests/{}", id);
        thirdPartyService.storeConsentRequestSync(id, body);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /validateAuthToken
     */
    @PostMapping("/validateAuthToken")
    public ResponseEntity<Map<String, Object>> validateAuthToken(@RequestBody Map<String, Object> body) {
        String id = (String) body.get("consentRequestId");
        log.info("POST /validateAuthToken id={}", id);
        // O SDK espera { isValid: true }
        return ResponseEntity.ok(Map.of("isValid", true));
    }

    /**
     * POST /store/consent
     */
    @PostMapping("/store/consent")
    public ResponseEntity<Void> storeConsent(@RequestBody Map<String, Object> body) {
        log.info("POST /store/consent id={}", body.get("consentId"));
        thirdPartyService.handleConsent(body, "central-auth");
        return ResponseEntity.ok().build();
    }

    // ─── TRANSFER ────────────────────────────────────────────────────────────

    /**
     * POST /validate-thirdparty-transaction-request
     */
    @PostMapping("/validate-thirdparty-transaction-request")
    public ResponseEntity<Map<String, Object>> validateTransactionRequest(@RequestBody Map<String, Object> body) {
        log.info("POST /validate-thirdparty-transaction-request id={}", body.get("transactionRequestId"));
        Map<String, Object> response = new HashMap<>();
        response.put("isValid", true);
        response.put("transactionRequestId", body.get("transactionRequestId"));
        response.put("consentId", "caixa-consent-456");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /verify-authorization
     */
    @PostMapping("/verify-authorization")
    public ResponseEntity<Map<String, Object>> verifyAuthorization(@RequestBody Map<String, Object> body) {
        log.info("POST /verify-authorization");
        return ResponseEntity.ok(Map.of("isValid", true));
    }
}
