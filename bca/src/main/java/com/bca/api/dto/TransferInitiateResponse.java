package com.bca.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferInitiateResponse {
    private String transferId;
    private PartyInfo party;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartyInfo {
        private String name;
        private String account;  // recipient identifier (MSISDN or account number)
        private String fspId;
    }
}
