package com.met.mto.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderExportTaskResponse {

    private Long id;
    private String taskNo;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String fileName;
    private Long fileSize;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
