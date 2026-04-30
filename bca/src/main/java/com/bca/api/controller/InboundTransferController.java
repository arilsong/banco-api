package com.bca.api.controller;

import com.bca.api.dto.TransferRequest;
import com.bca.api.dto.TransferResponse;
import com.bca.api.service.LedgerService;
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
