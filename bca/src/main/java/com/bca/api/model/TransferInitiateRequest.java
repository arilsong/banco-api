package com.bca.api.model;

import lombok.Data;

@Data
public class TransferInitiateRequest {
    private String fromMsisdn;
    private String toMsisdn;
    private String currency;
    private String amount;
}

