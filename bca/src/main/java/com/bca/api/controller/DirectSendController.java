package com.bca.api.controller;

import com.bca.api.core.repository.CoreAccountRepository;
import com.bca.api.dto.OutboundTransferRequest;
import com.bca.api.service.PartyService;
import com.bca.api.service.SdkOutboundClientService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/send")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DirectSendController {

    private final SdkOutboundClientService sdkClient;
    private final PartyService partyService;
    private final CoreAccountRepository accountRepository;

    /** Lookup de destinatário via SDK — GET /send/parties/{type}/{id} */
    @GetMapping("/parties/{type}/{id}")
    public ResponseEntity<Object> lookupParty(@PathVariable String type, @PathVariable String id) {
        log.info("Lookup direto: {}/{}", type, id);
        Object party = sdkClient.lookupParty(type, id);
        if (party != null) return ResponseEntity.ok(party);
        return ResponseEntity.notFound().build();
    }

    /** Solicitar cotação via SDK — POST /send/quotes */
    @PostMapping("/quotes")
    public ResponseEntity<Object> requestQuote(@RequestBody Map<String, Object> quoteRequest) {
        log.info("Quote direto: {}", quoteRequest);
        Object quote = sdkClient.requestQuote(quoteRequest);
        if (quote != null) return ResponseEntity.ok(quote);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha ao obter cotação.");
    }

    /** Envio direto (single-shot) — POST /send */
    @PostMapping
    @Transactional
    public ResponseEntity<String> sendTransfer(@RequestBody OutboundTransferRequest request) {
        log.info("Envio direto: {} → {}", request.getFromIdValue(), request.getToIdValue());

        BigDecimal txAmount = new BigDecimal(request.getAmount());

        var payer = partyService.findAccountByIdType(request.getFromIdType(), request.getFromIdValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta de origem não encontrada."));

        if (payer.getBalance().compareTo(txAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente.");
        }

        payer.setBalance(payer.getBalance().subtract(txAmount));
        accountRepository.save(payer);

        boolean success = sdkClient.sendTransferToHub(
                request.getFromIdType(), request.getFromIdValue(),
                request.getToIdType(), request.getToIdValue(),
                request.getCurrency(), request.getAmount()
        );

        if (success) {
            log.info("Envio direto concluído com sucesso.");
            return ResponseEntity.ok("Transferência enviada com sucesso.");
        } else {
            log.error("Envio direto falhou. Estornando saldo.");
            payer.setBalance(payer.getBalance().add(txAmount));
            accountRepository.save(payer);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "A transferência falhou. Valores estornados.");
        }
    }
}
