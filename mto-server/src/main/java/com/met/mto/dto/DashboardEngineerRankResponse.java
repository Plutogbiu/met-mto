package com.met.mto.dto;

import lombok.Data;

@Data
public class DashboardEngineerRankResponse {

    private Long userId;

    private String engineerName;

    private long workOrderCount;

    private long completedCount;
}
