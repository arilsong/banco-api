package com.caixa.api.model;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuoteResponse {
    private String quoteId;
    private String transactionId;
    private String transferAmount;
    private String transferAmountCurrency;
    private String payeeReceiveAmount;
    private String payeeReceiveAmountCurrency;
    private String payeeFspFee;
    private String payeeFspFeeCurrency;
    private String payeeFspCommission;
    private String payeeFspCommissionCurrency;
    private String expiration;

    // Helper class mantido para compatibilidade interna
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Amount {
        private String currency;
        private String amount;
    }
}

