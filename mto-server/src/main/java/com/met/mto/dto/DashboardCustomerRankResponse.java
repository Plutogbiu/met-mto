package com.met.mto.dto;

import lombok.Data;

@Data
public class DashboardCustomerRankResponse {

    private Long customerSiteId;

    private String customerSiteName;

    private long workOrderCount;
}
