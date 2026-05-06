package com.bca.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bca.api.dto.BulkTransferItemRequest;
import com.bca.api.dto.BulkTransferResponse;
import com.bca.api.service.BulkTransferService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
@RequestMapping("/send")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BulkSendController {
    private final BulkTransferService bulkTransferService;

    @PostMapping("/bulk")
    public ResponseEntity<BulkTransferResponse> sendBulk(
            @RequestBody List<BulkTransferItemRequest> items) {

        log.info("POST /send/bulk recebido com {} itens", items.size());

        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        BulkTransferResponse response = bulkTransferService.processBulk(items);
        return ResponseEntity.ok(response);
    }
}
