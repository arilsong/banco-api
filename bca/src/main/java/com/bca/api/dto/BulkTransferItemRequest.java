package com.bca.api.dto;

import lombok.Data;
//Respresenta uma transfirencia indivdual dentro do bulk
@Data
public class BulkTransferItemRequest {
    private String fromIdType;
    private String fromIdValue;
    private String toIdType;
    private String toIdValue;
    private String amount;
    private String currency;
}
