package com.bca.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

//Resultado de uma transfirencia individual dentro 
@Data
@AllArgsConstructor
public class BulkTransferItemResult {
    private String fromIdValue;
    private String toIdValue;
    private String amount;
    private String currency;
    private String status;
    private String reason;

}
