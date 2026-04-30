package com.caixa.api.dto;

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
    private QuoteInfo quote;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteInfo {
        private String transferAmount;
        private String currency;
        private String fee;
        private String expiration;
    }
}
