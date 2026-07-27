package com.met.mto.dto;

import lombok.Data;

@Data
public class DashboardWorkOrderTrendResponse {

    private String period;

    private String label;

    private long createdCount;

    private long completedCount;
}
