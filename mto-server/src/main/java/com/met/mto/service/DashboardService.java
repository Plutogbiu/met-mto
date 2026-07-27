package com.met.mto.service;

import com.met.mto.dto.DashboardOverviewResponse;
import java.time.LocalDate;

public interface DashboardService {

    DashboardOverviewResponse getOverview(LocalDate startDate, LocalDate endDate, String type);
}
