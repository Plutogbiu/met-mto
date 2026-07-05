package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("customer_device")
public class CustomerDevice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String model;

    private String serialNo;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
