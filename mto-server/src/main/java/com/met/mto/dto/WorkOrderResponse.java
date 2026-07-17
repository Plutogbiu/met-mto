package com.met.mto.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class WorkOrderResponse {

    private Long id;

    private String orderNo;

    private String title;

    private String type;

    private Long customerSiteId;

    private String customerSiteName;

    private String customerAddress;

    private Long deviceId;

    private String deviceName;

    private String priority;

    private String status;

    private String maintenanceContent;

    private String content;

    private LocalDateTime estimatedArrivalTime;

    private LocalDateTime estimatedCompleteTime;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<WorkOrderEngineerResponse> engineers;
}
