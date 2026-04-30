package com.caixa.api.controller;

import com.caixa.api.dto.*;
import com.caixa.api.service.AppTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppTransferController {

    private final AppTransferService transferService;

    /** Passo 1: Iniciar transferência (Lookup) — POST /transfer */
    @PostMapping
    public ResponseEntity<TransferInitiateResponse> initiateTransfer(@RequestBody TransferInitiateRequest request) {
        return ResponseEntity.ok(transferService.initiateTransfer(request));
    }

    /** Passo 2: Confirmar destinatário e obter cotação — PUT /transfer/{transferId}/confirm-party */
    @PutMapping("/{transferId}/confirm-party")
    public ResponseEntity<TransferConfirmPartyResponse> confirmParty(@PathVariable String transferId) {
        return ResponseEntity.ok(transferService.confirmParty(transferId));
    }

    /** Passo 3: Confirmar taxa e executar transferência — PUT /transfer/{transferId}/confirm-quote */
    @PutMapping("/{transferId}/confirm-quote")
    public ResponseEntity<TransferConfirmQuoteResponse> confirmQuote(@PathVariable String transferId) {
        return ResponseEntity.ok(transferService.confirmQuote(transferId));
    }
}
