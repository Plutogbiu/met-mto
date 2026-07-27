package com.met.mto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkOrderReceiptFile {

    private String fileName;

    private byte[] content;
}
