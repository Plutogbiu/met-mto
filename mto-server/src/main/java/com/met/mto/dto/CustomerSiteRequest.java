package com.met.mto.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CustomerSiteRequest {

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
}
