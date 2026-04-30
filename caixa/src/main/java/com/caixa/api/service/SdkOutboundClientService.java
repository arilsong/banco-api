package com.caixa.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SdkOutboundClientService {

    @Value("${sdk.outbound.url}")
    private String sdkOutboundUrl;

    private final LedgerService ledgerService;

    public Map<String, Object> lookupParty(String idType, String idValue) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            log.info("A consultar destinatário no HUB via SDK (CAIXA): {}/{}", idType, idValue);
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    sdkOutboundUrl + "/parties/" + idType + "/" + idValue, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro no Lookup Party (CAIXA): {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> requestQuote(Map<String, Object> quoteRequest) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            log.info("A solicitar cotação ao HUB via SDK (CAIXA): {}", quoteRequest);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    sdkOutboundUrl + "/quotes", quoteRequest, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Erro ao solicitar Quote (CAIXA): {}", e.getMessage());
            return null;
        }
    }

    public boolean sendTransferToHub(String fromIdType, String fromIdValue, 
                                     String toIdType, String toIdValue, 
                                     String currency, String amount) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("homeTransactionId", UUID.randomUUID().toString());
        
        Map<String, String> fromNode = new HashMap<>();
        fromNode.put("idType", fromIdType);
        fromNode.put("idValue", fromIdValue);
        requestBody.put("from", fromNode);

        Map<String, String> toNode = new HashMap<>();
        toNode.put("idType", toIdType);
        toNode.put("idValue", toIdValue);
        requestBody.put("to", toNode);

        requestBody.put("amountType", "SEND");
        requestBody.put("currency", currency);
        requestBody.put("amount", amount);
        requestBody.put("transactionType", "TRANSFER");

        try {
            log.info("A enviar pedido OUTBOUND (síncrono) para o SDK Scheme Adapter [CAIXA]: {}", requestBody);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    sdkOutboundUrl + "/transfers", requestBody, Map.class); // <-- Map em vez de String

            Map<String, Object> body = response.getBody();

            if (body == null) return false;

            String transferState = (String) body.get("transferState");
            String currentState  = (String) body.get("currentState");
            String transferId    = (String) body.get("transferId");

            log.info("Resposta do SDK [CAIXA]: transferState={}, currentState={}, transferId={}", transferState, currentState, transferId);

            boolean isSuccessful = "COMMITTED".equals(transferState) || "COMPLETED".equals(currentState);

            if (isSuccessful) {
                log.info("[CAIXA] Transferência concluída com sucesso. Processando débito local síncrono para conta {}", fromIdValue);
                com.caixa.api.dto.TransferRequest localRequest = new com.caixa.api.dto.TransferRequest();
                localRequest.setTransferId(transferId);
                localRequest.setAmount(amount);
                localRequest.setCurrency(currency);
                
                com.caixa.api.dto.QuoteRequest.PartyId from = new com.caixa.api.dto.QuoteRequest.PartyId();
                from.setIdType(fromIdType);
                from.setIdValue(fromIdValue);
                localRequest.setFrom(from);
                
                com.caixa.api.dto.QuoteRequest.PartyId to = new com.caixa.api.dto.QuoteRequest.PartyId();
                to.setIdType(toIdType);
                to.setIdValue(toIdValue);
                localRequest.setTo(to);

                ledgerService.processTransfer(localRequest);
            }

            return isSuccessful;

        } catch (Exception e) {
            log.error("Falha ao comunicar com o SDK Scheme Adapter (Outbound CAIXA): {}", e.getMessage());
            return false;
        }
    }
}

