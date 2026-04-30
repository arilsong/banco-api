package com.caixa.api.model;

import lombok.Data;

@Data
public class OutboundTransferRequest {
    private String fromIdType;
    private String fromIdValue;
    private String toIdType;
    private String toIdValue;
    private String currency;
    private String amount;
}

