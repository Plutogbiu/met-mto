package com.met.mto.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class WorkOrderRequest {

    private String type;

    private Long customerSiteId;

    private Long deviceId;

    private String priority;

    private String status;

    private String maintenanceContent;

    private String content;

    private LocalDateTime estimatedArrivalTime;

    private LocalDateTime estimatedCompleteTime;

    private List<Long> engineerIds;
}
