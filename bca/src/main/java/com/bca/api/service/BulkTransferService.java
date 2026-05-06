package com.bca.api.service;

import com.bca.api.core.repository.CoreAccountRepository;
import com.bca.api.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkTransferService {

    private final SdkOutboundClientService sdkClient;
    private final PartyService partyService;
    private final CoreAccountRepository accountRepository;

    public BulkTransferResponse processBulk(List<BulkTransferItemRequest> items) {

        // bulkId (Rastreabilidade)
        String bulkId = UUID.randomUUID().toString();
        log.info("=== BULK INICIADO [{}] — {} transfers ===", bulkId, items.size());

        List<BulkTransferItemResult> results = new ArrayList<>();

        int success = 0;
        int failed = 0;

        for (BulkTransferItemRequest item : items) {

            BulkTransferItemResult result = processItem(item);

            results.add(result);

            if ("SENT".equals(result.getStatus())) {
                success++;
            } else {
                failed++;
            }
        }

        log.info("=== BULK CONCLUÍDO [{}] OK:{} FAIL:{} ===",
                bulkId, success, failed);

        return new BulkTransferResponse(
                bulkId,
                items.size(),
                success,
                failed,
                results);
    }

    // ITEM PROCESSING COM QUOTE
    private BulkTransferItemResult processItem(BulkTransferItemRequest item) {

        log.info("→ {} para {}", item.getFromIdValue(), item.getToIdValue());

        BigDecimal amount;

        // validar amount
        try {
            amount = new BigDecimal(item.getAmount());
        } catch (Exception e) {
            return fail(item, "Amount inválido");
        }

        // valida conta
        var payer = partyService
                .findAccountByIdType(item.getFromIdType(), item.getFromIdValue())
                .orElse(null);

        if (payer == null) {
            return fail(item, "Conta origem não existe");
        }

        // saldo
        if (payer.getBalance().compareTo(amount) < 0) {
            return fail(item, "Saldo insuficiente");
        }

        // QUOTE (NOVA FASE)
        try {
            Object quote = sdkClient.requestQuote(
                    java.util.Map.of(
                            "from", item.getFromIdValue(),
                            "to", item.getToIdValue(),
                            "amount", item.getAmount(),
                            "currency", item.getCurrency()));

            if (quote == null) {
                return fail(item, "Quote rejeitada");
            }

        } catch (Exception e) {
            return fail(item, "Erro ao obter quote");
        }

        // debitar
        payer.setBalance(payer.getBalance().subtract(amount));
        accountRepository.save(payer);

        // enviar (SDK)
        boolean sent = sdkClient.sendTransferToHub(
                item.getFromIdType(), item.getFromIdValue(),
                item.getToIdType(), item.getToIdValue(),
                item.getCurrency(), item.getAmount());

        // status MAJORADO
        if (sent) {
            return new BulkTransferItemResult(
                    item.getFromIdValue(),
                    item.getToIdValue(),
                    item.getAmount(),
                    item.getCurrency(),
                    "SENT",
                    null);
        }

        // rollback
        payer.setBalance(payer.getBalance().add(amount));
        accountRepository.save(payer);

        return fail(item, "Falha no envio para Hub");
    }

    private BulkTransferItemResult fail(BulkTransferItemRequest item, String reason) {
        return new BulkTransferItemResult(
                item.getFromIdValue(),
                item.getToIdValue(),
                item.getAmount(),
                item.getCurrency(),
                "FAILED",
                reason);
    }
}