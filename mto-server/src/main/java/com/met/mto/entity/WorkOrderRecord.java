package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_record")
public class WorkOrderRecord {

    @TableId(type = IdType.AUTO)
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
}
