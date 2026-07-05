package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("customer_site")
public class CustomerSite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String address;

    private String contactName;

    private String contactPhone;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String locationAddress;

    private String locationRemark;

    private String remark;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
