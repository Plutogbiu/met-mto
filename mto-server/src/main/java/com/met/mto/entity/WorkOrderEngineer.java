package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("work_order_engineer")
public class WorkOrderEngineer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workOrderId;

    private Long userId;

    private String username;

    private String realName;

    private LocalDateTime createdAt;
}
