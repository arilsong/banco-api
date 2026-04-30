package com.bca.api.service;

import com.bca.api.core.model.CoreAccount;
import com.bca.api.core.repository.CoreAccountRepository;
import com.bca.api.dto.*;
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
                .filter(acc -> acc.getAccountNumber().equals(request.getFromAccount())
                            || acc.getMsisdn().equals(request.getFromAccount()))
                .findFirst()
                .orElse(null);
        String fromMsisdn = senderAccount != null ? senderAccount.getMsisdn() : request.getFromAccount();

        String toIdType = (request.getToAccountType() != null && !request.getToAccountType().isBlank())
                ? request.getToAccountType().toUpperCase()
                : "MSISDN";

        Map<String, Object> body = new HashMap<>();
        body.put("homeTransactionId", UUID.randomUUID().toString());
        body.put("from", Map.of("idType", "MSISDN", "idValue", fromMsisdn));
        body.put("to",   Map.of("idType", toIdType,  "idValue", request.getToAccount()));
        body.put("amountType", "SEND");
        body.put("currency", request.getCurrency());
        body.put("amount", new BigDecimal(request.getAmount()).stripTrailingZeros().toPlainString());
        body.put("transactionType", "TRANSFER");

        try {
            log.info("Passo 1 (Lookup) no SDK: from={} to={}", fromMsisdn, request.getToAccount());
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
            log.error("Erro no Passo 1: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Passo 2: Confirmar destinatário (acceptParty) e receber cotação do SDK.
     */
    public TransferConfirmPartyResponse confirmParty(String transferId) {
        Map<String, Object> body = Map.of("acceptParty", true);
        try {
            log.info("Passo 2 (Accept Party) no SDK para transferId={}", transferId);
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

            return TransferConfirmPartyResponse.builder()
                    .transferId((String) respBody.get("transferId"))
                    .quote(TransferConfirmPartyResponse.QuoteInfo.builder()
                            .transferAmount((String) amountMap.get("amount"))
                            .currency((String) amountMap.get("currency"))
                            .fee(feeMap != null ? (String) feeMap.get("amount") : "0")
                            .expiration((String) quoteBody.get("expiration"))
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Erro no Passo 2: {}", e.getMessage());
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
            log.info("Passo 3 (Accept Quote) no SDK para transferId={}", transferId);
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

            if ("COMMITTED".equals(currentState) || "COMPLETED".equals(currentState)) {
                log.info("Passo 3 concluído. Registrando débito local para transferId={}", transferId);

                TransferRequest localReq = new TransferRequest();
                localReq.setTransferId((String) respBody.get("transferId"));
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
                    .transferId((String) respBody.get("transferId"))
                    .status(currentState)
                    .build();
        } catch (Exception e) {
            log.error("Erro no Passo 3: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
