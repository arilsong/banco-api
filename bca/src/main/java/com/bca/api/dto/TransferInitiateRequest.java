package com.bca.api.dto;

import lombok.Data;

@Data
public class TransferInitiateRequest {
    private String fromAccount;    // sender account ID or MSISDN
    private String toAccount;      // recipient identifier value
    private String toAccountType;  // oracle type: "MSISDN" (P2P, default) or "BUSINESS"
    private String currency;
    private String amount;
}
