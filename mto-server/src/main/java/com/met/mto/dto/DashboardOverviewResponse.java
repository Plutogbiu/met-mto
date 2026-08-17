package com.met.mto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class DashboardOverviewResponse {

    private LocalDate startDate;

    private LocalDate endDate;

    private long totalCount;

    private long pendingCount;

    private long processingCount;

    private long completedCount;

    private long closedCount;

    private BigDecimal completionRate;

    private List<DashboardCustomerRankResponse> customerRanks;

    private String trendUnit;

    private List<DashboardWorkOrderTrendResponse> trends;
}
