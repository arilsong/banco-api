package com.bni.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

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
}
