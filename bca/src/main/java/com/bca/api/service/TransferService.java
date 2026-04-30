package com.bca.api.service;


import com.bca.api.core.model.CoreAccount;
import com.bca.api.core.repository.CoreAccountRepository;
import com.bca.api.dto.TransferRequest;
import com.bca.api.dto.TransferResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final PartyService partyService;
    private final CoreAccountRepository accountRepository;
    private final com.bca.api.core.repository.CoreTransactionRepository transactionRepository;

    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        log.info("Processando TransferRequest (Webhook): transferId={}, from={}, to={}", 
                 request.getTransferId(), request.getFrom(), request.getTo());

        BigDecimal txAmount = extractAmount(request);
        if (txAmount == null) {
            log.warn("Valor da transferência não encontrado no request para ID: {}", request.getTransferId());
            // Se não encontrar o valor, não conseguiremos processar o débito/crédito
            return TransferResponse.builder()
                .homeTransactionId("bca-error-" + UUID.randomUUID().toString().substring(0, 8))
                .transferState("ERROR_OCCURRED")
                .build();
        }

        String currency = request.getCurrency() != null ? request.getCurrency() : "CVE";

        // 1. Processar Débito (se o Remetente for local - este banco é o Payer)
        if (request.getFrom() != null) {
            String fromType = request.getFrom().getIdType();
            String fromId = request.getFrom().getIdValue();
            
            partyService.findAccountByIdType(fromType, fromId).ifPresent(payer -> {
                log.info("Processando Débito (Outbound Transfer): Debitar {} {} da conta {} (Ref ALIAS {})", 
                            txAmount, currency, payer.getAccountNumber(), fromId);
                payer.setBalance(payer.getBalance().subtract(txAmount));
                accountRepository.save(payer);

                // Gravar Histórico de Transação
                transactionRepository.save(com.bca.api.core.model.CoreTransaction.builder()
                        .transactionId(request.getTransferId())
                        .description("Transferência Enviada para " + (request.getTo() != null ? request.getTo().getIdValue() : "Externo"))
                        .amount(txAmount.negate())
                        .currency(currency)
                        .timestamp(java.time.LocalDateTime.now())
                        .account(payer)
                        .build());
            });
        }

        // 2. Processar Crédito (se o Destinatário for local - este banco é o Payee)
        if (request.getTo() != null) {
            String toType = request.getTo().getIdType();
            String toId = request.getTo().getIdValue();
            
            partyService.findAccountByIdType(toType, toId).ifPresent(payee -> {
                log.info("Processando Crédito (Inbound Transfer): Creditar {} {} na conta {} (Ref ALIAS {})", 
                            txAmount, currency, payee.getAccountNumber(), toId);
                payee.setBalance(payee.getBalance().add(txAmount));
                accountRepository.save(payee);

                // Gravar Histórico de Transação
                transactionRepository.save(com.bca.api.core.model.CoreTransaction.builder()
                        .transactionId(request.getTransferId())
                        .description("Transferência Recebida de " + (request.getFrom() != null ? request.getFrom().getIdValue() : "Externo"))
                        .amount(txAmount)
                        .currency(currency)
                        .timestamp(java.time.LocalDateTime.now())
                        .account(payee)
                        .build());
            });
        }

        String homeTxId = "bca-core-" + UUID.randomUUID().toString().substring(0, 8);
        return TransferResponse.builder()
                .homeTransactionId(homeTxId)
                .transferState("COMMITTED")
                .build();
    }

    private BigDecimal extractAmount(TransferRequest request) {
        try {
            if (request.getAmount() != null) {
                return new BigDecimal(request.getAmount());
            }
            if (request.getQuote() != null) {
                Map<String, Object> quote = request.getQuote();
                if (quote.containsKey("transferAmount")) {
                    Map<String, Object> transferAmount = (Map<String, Object>) quote.get("transferAmount");
                    if (transferAmount != null && transferAmount.get("amount") != null) {
                        return new BigDecimal(transferAmount.get("amount").toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erro ao extrair valor da transferência: {}", e.getMessage());
        }
        return null;
    }
}

