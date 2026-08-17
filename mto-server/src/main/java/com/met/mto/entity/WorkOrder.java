package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order")
public class WorkOrder {

    @TableId(type = IdType.AUTO)
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
}
