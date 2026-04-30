package com.bca.api.service;

import com.bca.api.core.model.CoreAccount;
import com.bca.api.core.model.CoreTransaction;
import com.bca.api.core.repository.CoreAccountRepository;
import com.bca.api.core.repository.CoreTransactionRepository;
import com.bca.api.dto.TransferRequest;
import com.bca.api.dto.TransferResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final PartyService partyService;
    private final CoreAccountRepository accountRepository;
    private final CoreTransactionRepository transactionRepository;

    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        log.info("Processando débito/crédito no ledger: transferId={}", request.getTransferId());

        BigDecimal txAmount = extractAmount(request);
        if (txAmount == null) {
            log.warn("Valor não encontrado para transferId={}", request.getTransferId());
            return TransferResponse.builder()
                    .homeTransactionId("ledger-error-" + UUID.randomUUID().toString().substring(0, 8))
                    .transferState("ERROR_OCCURRED")
                    .build();
        }

        String currency = request.getCurrency() != null ? request.getCurrency() : "CVE";

        // Débito — se o remetente for conta local (banco é Payer)
        if (request.getFrom() != null) {
            String fromType = request.getFrom().getIdType();
            String fromId   = request.getFrom().getIdValue();

            partyService.findAccountByIdType(fromType, fromId).ifPresent(payer -> {
                log.info("Debitando {} {} da conta {}", txAmount, currency, payer.getAccountNumber());
                payer.setBalance(payer.getBalance().subtract(txAmount));
                accountRepository.save(payer);

                transactionRepository.save(CoreTransaction.builder()
                        .transactionId(request.getTransferId())
                        .description("Transferência Enviada para " +
                                (request.getTo() != null ? request.getTo().getIdValue() : "Externo"))
                        .amount(txAmount.negate())
                        .currency(currency)
                        .timestamp(LocalDateTime.now())
                        .account(payer)
                        .build());
            });
        }

        // Crédito — se o destinatário for conta local (banco é Payee)
        if (request.getTo() != null) {
            String toType = request.getTo().getIdType();
            String toId   = request.getTo().getIdValue();

            partyService.findAccountByIdType(toType, toId).ifPresent(payee -> {
                log.info("Creditando {} {} na conta {}", txAmount, currency, payee.getAccountNumber());
                payee.setBalance(payee.getBalance().add(txAmount));
                accountRepository.save(payee);

                transactionRepository.save(CoreTransaction.builder()
                        .transactionId(request.getTransferId())
                        .description("Transferência Recebida de " +
                                (request.getFrom() != null ? request.getFrom().getIdValue() : "Externo"))
                        .amount(txAmount)
                        .currency(currency)
                        .timestamp(LocalDateTime.now())
                        .account(payee)
                        .build());
            });
        }

        return TransferResponse.builder()
                .homeTransactionId("ledger-" + UUID.randomUUID().toString().substring(0, 8))
                .transferState("COMMITTED")
                .build();
    }

    private BigDecimal extractAmount(TransferRequest request) {
        try {
            if (request.getAmount() != null) return new BigDecimal(request.getAmount());
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
            log.error("Erro ao extrair valor: {}", e.getMessage());
        }
        return null;
    }
}
