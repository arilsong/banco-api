package com.bca.api.model;

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
    private String status;
    private PartyInfo party;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartyInfo {
        private String name;
        private String msisdn;
        private String fspId;
    }
}

