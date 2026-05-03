package com.caixa.api.controller;

import com.caixa.api.dto.FxQuoteRequest;
import com.caixa.api.dto.FxTransferRequest;
import com.caixa.api.service.FxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FxController {

    private final FxService fxService;

    @PostMapping("/fxQuotes")
    public ResponseEntity<Map<String, Object>> fxQuote(@RequestBody FxQuoteRequest request) {
        return ResponseEntity.ok(fxService.processQuote(request));
    }

    @PostMapping("/fxTransfers")
    public ResponseEntity<Map<String, Object>> reserveTransfer(@RequestBody FxTransferRequest request) {
        return ResponseEntity.ok(fxService.reserveTransfer(request));
    }

    @PutMapping("/fxTransfers/{commitRequestId}")
    public ResponseEntity<Map<String, Object>> commitTransfer(@PathVariable String commitRequestId) {
        return ResponseEntity.ok(fxService.commitTransfer(commitRequestId));
    }

    @PatchMapping("/fxTransfers/{commitRequestId}")
    public ResponseEntity<Map<String, Object>> abortTransfer(@PathVariable String commitRequestId) {
        return ResponseEntity.ok(fxService.abortTransfer(commitRequestId));
    }
}
