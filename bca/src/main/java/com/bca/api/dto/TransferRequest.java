package com.bca.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferRequest {
    private String transferId;
    private Map<String, Object> quote;
    private QuoteRequest.PartyId from;
    private QuoteRequest.PartyId to;
    private String amountType;
    private String currency;
    private String amount;
    private String transactionType;
    private String note;
    private String homeTransactionId;
}

