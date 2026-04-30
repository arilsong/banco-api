package com.caixa.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteRequest {
    private String quoteId;
    private String transactionId;
    private PartyId to;
    private PartyId from;
    private String amountType;
    private Object amount;
    private String currency;
    private String transactionType;
    private String initiator;
    private String initiatorType;
    private String note;
    private String expiration;


    @Data
    public static class PartyId {
        private String idType;
        private String idValue;
    }

    @Data
    public static class Amount {
        private String currency;
        private String amount;
    }

    public Amount getAmountParsed() {
        Amount a = new Amount();
        if (amount instanceof String) {
            a.setAmount((String) amount);
            a.setCurrency(this.currency);
        } else if (amount instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) amount;
            a.setAmount(m.get("amount") != null ? m.get("amount").toString() : null);
            a.setCurrency(m.get("currency") != null ? m.get("currency").toString() : this.currency);
        }
        return a;
    }
}
