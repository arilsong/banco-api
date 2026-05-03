package com.bca.api.dto;

import lombok.Data;

@Data
public class FxTransferRequest {

    private String commitRequestId;
    private String determiningTransferId;
    private String initiatingFsp;
    private String counterPartyFsp;
    private String amountType;
    private AmountField sourceAmount;
    private AmountField targetAmount;
    private String condition;
    private String expiration;

    @Data
    public static class AmountField {
        private String currency;
        private String amount;
    }
}
