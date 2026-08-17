package com.met.mto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkOrderReceiptImage {

    private String category;
    private String categoryLabel;
    private String dataUri;
    private String originalName;
}
