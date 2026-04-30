package com.bca.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferConfirmPartyResponse {
    private String transferId;
    private String status;
    private QuoteInfo quote;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteInfo {
        private String transferAmount;
        private String transferAmountCurrency;
        private String payeeFspFee;
        private String payeeFspFeeCurrency;
        private String expiration;
    }
}

