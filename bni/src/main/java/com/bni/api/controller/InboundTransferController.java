package com.bni.api.controller;

import com.bni.api.dto.TransferRequest;
import com.bni.api.dto.TransferResponse;
import com.bni.api.service.LedgerService;
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

    @PostMapping
    public ResponseEntity<Map<String, String>> createTransfer(@RequestBody TransferRequest request) {
        TransferResponse response = ledgerService.processTransfer(request);
        return ResponseEntity.ok(Map.of("homeTransactionId", response.getHomeTransactionId()));
    }
}
