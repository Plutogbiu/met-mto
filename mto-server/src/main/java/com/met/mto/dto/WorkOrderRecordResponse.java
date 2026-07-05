package com.met.mto.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class WorkOrderRecordResponse {

    private Long id;

    private Long workOrderId;

    private String recordType;

    private String statusBefore;

    private String statusAfter;

    private String content;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String locationAddress;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime createdAt;

    private List<FileAttachmentResponse> attachments;
}
