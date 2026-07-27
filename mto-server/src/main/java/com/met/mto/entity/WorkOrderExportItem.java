package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_export_item")
public class WorkOrderExportItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long workOrderId;
    private String orderNo;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
