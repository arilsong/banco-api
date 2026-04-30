package com.caixa.api.controller;

import com.caixa.api.dto.TransferRequest;
import com.caixa.api.dto.TransferResponse;
import com.caixa.api.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InboundTransferController {

    private final LedgerService ledgerService;

    /** Webhook chamado pelo Mojaloop SDK quando uma transferência de entrada é finalizada */
    @PostMapping
    public ResponseEntity<Map<String, String>> createTransfer(@RequestBody TransferRequest request) {
        TransferResponse response = ledgerService.processTransfer(request);
        return ResponseEntity.ok(Map.of("homeTransactionId", response.getHomeTransactionId()));
    }
}
