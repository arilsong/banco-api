package com.caixa.api.service;

import com.caixa.api.core.model.CoreAccount;
import com.caixa.api.core.repository.CoreAccountRepository;
import com.caixa.api.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppTransferService {

    @Value("${sdk.outbound.url}")
    private String sdkOutboundUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final LedgerService ledgerService;
    private final CoreAccountRepository accountRepository;

    /**
     * Passo 1: Iniciar transferência — chama SDK para fazer party lookup.
     * Recebe fromAccount (account ID ou MSISDN) e toAccount (identificador do destinatário).
     */
    public TransferInitiateResponse initiateTransfer(TransferInitiateRequest request) {
        CoreAccount senderAccount = accountRepository.findAll().stream()
                .filter(acc -> request.getFromAccount().equals(acc.getAccountNumber())
                        || request.getFromAccount().equals(acc.getMsisdn())
                        || request.getFromAccount().equals(acc.getBusinessId()))
                .findFirst()
                .orElse(null);

        // Determine correct idType/idValue for the sender (from).
        // Business accounts use BUSINESS + businessId; consumers use MSISDN.
        final String fromIdType;
        final String fromIdValue;
        if (senderAccount != null && "BUSINESS".equals(senderAccount.getPartyType())
                && senderAccount.getBusinessId() != null && !senderAccount.getBusinessId().isBlank()) {
            fromIdType  = "BUSINESS";
            fromIdValue = senderAccount.getBusinessId();
        } else {
            fromIdType  = "MSISDN";
            fromIdValue = senderAccount != null ? senderAccount.getMsisdn() : request.getFromAccount();
        }

        String toIdType = (request.getToAccountType() != null && !request.getToAccountType().isBlank())
                ? request.getToAccountType().toUpperCase()
                : "MSISDN";

        Map<String, Object> body = new HashMap<>();
        body.put("homeTransactionId", UUID.randomUUID().toString());
        body.put("from", Map.of("idType", fromIdType, "idValue", fromIdValue));
        body.put("to",   Map.of("idType", toIdType,   "idValue", request.getToAccount()));
        body.put("amountType", "SEND");
        body.put("currency", request.getCurrency());
        body.put("amount", new BigDecimal(request.getAmount()).stripTrailingZeros().toPlainString());
        body.put("transactionType", "TRANSFER");

        try {
            log.info("Passo 1 (Lookup) no SDK [CAIXA]: from={}/{} to={}/{}", fromIdType, fromIdValue, toIdType, request.getToAccount());
            ResponseEntity<Map> response = restTemplate.postForEntity(sdkOutboundUrl + "/transfers", body, Map.class);
            Map<String, Object> respBody = response.getBody();

            if (respBody == null || "ERROR_OCCURRED".equals(respBody.get("currentState"))) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SDK retornou erro no Passo 1.");
            }

            Map<String, Object> partyMap = (Map<String, Object>) respBody.get("to");
            String firstName = (String) partyMap.getOrDefault("firstName", "");
            String lastName  = (String) partyMap.getOrDefault("lastName", "");
            String fullName  = (firstName + " " + lastName).trim();

            return TransferInitiateResponse.builder()
                    .transferId((String) respBody.get("transferId"))
                    .party(TransferInitiateResponse.PartyInfo.builder()
                            .name(fullName)
                            .account((String) partyMap.get("idValue"))
                            .fspId((String) partyMap.get("fspId"))
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Erro no Passo 1 [CAIXA]: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Passo 2: Confirmar destinatário (acceptParty) e receber cotação do SDK.
     */
    public TransferConfirmPartyResponse confirmParty(String transferId) {
        Map<String, Object> body = Map.of("acceptParty", true);
        try {
            log.info("Passo 2 (Accept Party) no SDK [CAIXA] para transferId={}", transferId);
            ResponseEntity<Map> response = restTemplate.exchange(
                    sdkOutboundUrl + "/transfers/" + transferId,
                    HttpMethod.PUT,
                    new HttpEntity<>(body),
                    Map.class
            );
            Map<String, Object> respBody = response.getBody();

            if (respBody == null || "ERROR_OCCURRED".equals(respBody.get("currentState"))) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SDK retornou erro no Passo 2.");
            }

            Map<String, Object> quoteMap  = (Map<String, Object>) respBody.get("quoteResponse");
            Map<String, Object> quoteBody = (Map<String, Object>) quoteMap.get("body");
            Map<String, Object> amountMap = (Map<String, Object>) quoteBody.get("transferAmount");
            Map<String, Object> feeMap    = (Map<String, Object>) quoteBody.get("payeeFspFee");
            Map<String, Object> payeeReceiveMap = (Map<String, Object>) quoteBody.get("payeeReceiveAmount");

            String sdkId2 = (String) respBody.get("transferId");
            return TransferConfirmPartyResponse.builder()
                    .transferId(sdkId2 != null && !sdkId2.isBlank() ? sdkId2 : transferId)
                    .quote(TransferConfirmPartyResponse.QuoteInfo.builder()
                            .transferAmount(new BigDecimal((String) amountMap.get("amount")).stripTrailingZeros().toPlainString())
                            .currency((String) amountMap.get("currency"))
                            .fee(feeMap != null ? new BigDecimal((String) feeMap.get("amount")).stripTrailingZeros().toPlainString() : "0")
                            .feeCurrency(feeMap != null ? (String) feeMap.get("currency") : (String) amountMap.get("currency"))
                            .payeeReceiveAmount(payeeReceiveMap != null ? new BigDecimal((String) payeeReceiveMap.get("amount")).stripTrailingZeros().toPlainString() : null)
                            .payeeReceiveAmountCurrency(payeeReceiveMap != null ? (String) payeeReceiveMap.get("currency") : null)
                            .expiration((String) quoteBody.get("expiration"))
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Erro no Passo 2 [CAIXA]: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Passo 3: Confirmar taxa (acceptQuote) e executar transferência no SDK.
     * Após sucesso, registra o débito no ledger local.
     */
    public TransferConfirmQuoteResponse confirmQuote(String transferId) {
        Map<String, Object> body = Map.of("acceptQuote", true);
        try {
            log.info("Passo 3 (Accept Quote) no SDK [CAIXA] para transferId={}", transferId);
            ResponseEntity<Map> response = restTemplate.exchange(
                    sdkOutboundUrl + "/transfers/" + transferId,
                    HttpMethod.PUT,
                    new HttpEntity<>(body),
                    Map.class
            );
            Map<String, Object> respBody = response.getBody();

            if (respBody == null || "ERROR_OCCURRED".equals(respBody.get("currentState"))) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SDK retornou erro no Passo 3.");
            }

            String currentState = (String) respBody.get("currentState");

            String sdkId3 = (String) respBody.get("transferId");
            String resolvedId = (sdkId3 != null && !sdkId3.isBlank()) ? sdkId3 : transferId;

            if ("COMMITTED".equals(currentState) || "COMPLETED".equals(currentState)) {
                log.info("Passo 3 concluído [CAIXA]. Registrando débito local para transferId={}", resolvedId);

                TransferRequest localReq = new TransferRequest();
                localReq.setTransferId(resolvedId);
                if (respBody.get("amount") != null) localReq.setAmount(respBody.get("amount").toString());
                if (respBody.get("currency") != null) localReq.setCurrency(respBody.get("currency").toString());

                if (respBody.containsKey("from")) {
                    Map<String, Object> fromMap = (Map<String, Object>) respBody.get("from");
                    QuoteRequest.PartyId from = new QuoteRequest.PartyId();
                    from.setIdType((String) fromMap.get("idType"));
                    from.setIdValue((String) fromMap.get("idValue"));
                    localReq.setFrom(from);
                }
                if (respBody.containsKey("quote")) {
                    localReq.setQuote((Map<String, Object>) respBody.get("quote"));
                }

                ledgerService.processTransfer(localReq);
            }

            return TransferConfirmQuoteResponse.builder()
                    .transferId(resolvedId)
                    .status(currentState)
                    .build();
        } catch (Exception e) {
            log.error("Erro no Passo 3 [CAIXA]: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
