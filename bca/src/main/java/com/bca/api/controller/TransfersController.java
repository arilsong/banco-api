package com.bca.api.controller;


import com.bca.api.dto.TransferRequest;
import com.bca.api.dto.TransferResponse;
import com.bca.api.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransfersController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createTransfer(@RequestBody TransferRequest request) {
        TransferResponse response = transferService.processTransfer(request);
        
        // Retorna apenas homeTransactionId conforme esperado pelo SDK Mock
        return ResponseEntity.ok(Map.of("homeTransactionId", response.getHomeTransactionId()));
    }
}

