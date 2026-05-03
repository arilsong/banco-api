package com.bni.api.dto;

import lombok.Data;

@Data
public class FxQuoteRequest {

    private String conversionRequestId;
    private ConversionTerms conversionTerms;

    @Data
    public static class ConversionTerms {
        private String conversionId;
        private String determiningTransferId;
        private String initiatingFsp;
        private String counterPartyFsp;
        private String amountType;
        private AmountField sourceAmount;
        private AmountField targetAmount;
        private String expiration;
    }

    @Data
    public static class AmountField {
        private String currency;
        private String amount;
    }
}
