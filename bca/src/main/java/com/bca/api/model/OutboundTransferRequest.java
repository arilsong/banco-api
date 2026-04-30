package com.bca.api.model;

import lombok.Data;

@Data
public class OutboundTransferRequest {
    private String fromIdType; // ex: MSISDN
    private String fromIdValue; // origem (seu cliente)
    private String toIdType;   // ex: MSISDN
    private String toIdValue;   // destino (cliente externo)
    private String currency; // ex: CVE
    private String amount;
}

