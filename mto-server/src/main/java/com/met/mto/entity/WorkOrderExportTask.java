package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_export_task")
public class WorkOrderExportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private Long creatorId;
    private String creatorName;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String fileName;
    private String storagePath;
    private Long fileSize;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
