package com.bni.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferResponse {
    private String homeTransactionId;
    private String transferId;
    private String completedTimestamp;
    private String transferState;
}
