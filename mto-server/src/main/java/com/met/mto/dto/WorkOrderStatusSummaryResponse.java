package com.met.mto.dto;

import lombok.Data;

@Data
public class WorkOrderStatusSummaryResponse {

    private long totalCount;

    private long pendingCount;

    private long processingCount;

    private long completedCount;

    private long closedCount;
}
