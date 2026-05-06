package com.bca.api.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

//Resposta consolidada do bulk transfer
@Data
@AllArgsConstructor
public class BulkTransferResponse {

    private String bulkId;

    private int total;
    private int success;
    private int failed;

    private List<BulkTransferItemResult> results;
}
