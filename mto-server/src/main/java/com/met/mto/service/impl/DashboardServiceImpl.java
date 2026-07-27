package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.met.mto.dto.DashboardCustomerRankResponse;
import com.met.mto.dto.DashboardOverviewResponse;
import com.met.mto.entity.WorkOrder;
import com.met.mto.mapper.WorkOrderMapper;
import com.met.mto.service.DashboardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final WorkOrderMapper workOrderMapper;

    @Override
    public DashboardOverviewResponse getOverview(LocalDate startDate, LocalDate endDate, String type) {
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();
        String normalizedType = type == null || type.trim().isEmpty() ? null : type;
        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setTotalCount(countByStatus(startTime, endTime, normalizedType, null));
        response.setPendingCount(countByStatus(startTime, endTime, normalizedType, "pending"));
        response.setProcessingCount(countByStatus(startTime, endTime, normalizedType, "processing"));
        response.setCompletedCount(countByStatus(startTime, endTime, normalizedType, "completed"));
        response.setClosedCount(countByStatus(startTime, endTime, normalizedType, "closed"));
        response.setCompletionRate(calculateCompletionRate(response.getCompletedCount(), response.getTotalCount()));
        response.setCustomerRanks(loadCustomerRanks(startTime, endTime, normalizedType));
        response.setTrendUnit(resolveTrendUnit(startDate, endDate));
        response.setTrends(loadTrends(startDate, endDate, normalizedType, response.getTrendUnit()));
        return response;
    }

    private long countByStatus(LocalDateTime startTime, LocalDateTime endTime, String type, String status) {
        return workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .ge(WorkOrder::getCreatedAt, startTime)
                .lt(WorkOrder::getCreatedAt, endTime)
                .eq(type != null, WorkOrder::getType, type)
                .eq(status != null, WorkOrder::getStatus, status));
    }

    private BigDecimal calculateCompletionRate(long completedCount, long totalCount) {
        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
    }

    private List<DashboardCustomerRankResponse> loadCustomerRanks(LocalDateTime startTime, LocalDateTime endTime, String type) {
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        wrapper.select(
                        "customer_site_id as customerSiteId",
                        "customer_site_name as customerSiteName",
                        "count(*) as workOrderCount")
                .ge("created_at", startTime)
                .lt("created_at", endTime)
                .eq(type != null, "type", type)
                .isNotNull("customer_site_id")
                .groupBy("customer_site_id", "customer_site_name")
                .orderByDesc("workOrderCount")
                .last("limit 10");

        List<Map<String, Object>> rows = workOrderMapper.selectMaps(wrapper);
        List<DashboardCustomerRankResponse> ranks = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            DashboardCustomerRankResponse rank = new DashboardCustomerRankResponse();
            rank.setCustomerSiteId(toLong(row.get("customerSiteId")));
            Object customerSiteName = row.get("customerSiteName");
            rank.setCustomerSiteName(customerSiteName == null ? "-" : customerSiteName.toString());
            rank.setWorkOrderCount(toLong(row.get("workOrderCount")));
            ranks.add(rank);
        }
        return ranks;
    }

    private List<DashboardWorkOrderTrendResponse> loadTrends(
            LocalDate startDate, LocalDate endDate, String type, String trendUnit) {
        Map<String, Long> createdCounts = loadTrendCounts("created_at", startDate, endDate, type, trendUnit);
        Map<String, Long> completedCounts = loadTrendCounts("completed_at", startDate, endDate, type, trendUnit);
        List<DashboardWorkOrderTrendResponse> trends = new ArrayList<>();
        LocalDate cursor = initialTrendDate(startDate, trendUnit);
        while (!cursor.isAfter(endDate)) {
            String period = formatTrendPeriod(cursor, trendUnit);
            DashboardWorkOrderTrendResponse trend = new DashboardWorkOrderTrendResponse();
            trend.setPeriod(period);
            trend.setLabel(formatTrendLabel(cursor, trendUnit));
            trend.setCreatedCount(createdCounts.containsKey(period) ? createdCounts.get(period) : 0L);
            trend.setCompletedCount(completedCounts.containsKey(period) ? completedCounts.get(period) : 0L);
            trends.add(trend);
            cursor = nextTrendDate(cursor, trendUnit);
        }
        return trends;
    }

    private Map<String, Long> loadTrendCounts(
            String timeColumn, LocalDate startDate, LocalDate endDate, String type, String trendUnit) {
        String periodExpression = trendPeriodExpression(timeColumn, trendUnit);
        QueryWrapper<WorkOrder> wrapper = new QueryWrapper<>();
        wrapper.select(periodExpression + " as period", "count(*) as workOrderCount")
                .ge(timeColumn, startDate.atStartOfDay())
                .lt(timeColumn, endDate.plusDays(1).atStartOfDay())
                .eq(type != null, "type", type)
                .groupBy(periodExpression)
                .orderByAsc(periodExpression);
        List<Map<String, Object>> rows = workOrderMapper.selectMaps(wrapper);
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            counts.put(String.valueOf(row.get("period")), toLong(row.get("workOrderCount")));
        }
        return counts;
    }

    private String resolveTrendUnit(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days <= 31) {
            return "day";
        }
        if (days <= 180) {
            return "week";
        }
        return "month";
    }

    private String trendPeriodExpression(String timeColumn, String trendUnit) {
        if ("week".equals(trendUnit)) {
            return "date_format(" + timeColumn + ", '%x-%v')";
        }
        if ("month".equals(trendUnit)) {
            return "date_format(" + timeColumn + ", '%Y-%m')";
        }
        return "date_format(" + timeColumn + ", '%Y-%m-%d')";
    }

    private LocalDate initialTrendDate(LocalDate startDate, String trendUnit) {
        if ("week".equals(trendUnit)) {
            return startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if ("month".equals(trendUnit)) {
            return startDate.withDayOfMonth(1);
        }
        return startDate;
    }

    private LocalDate nextTrendDate(LocalDate value, String trendUnit) {
        if ("week".equals(trendUnit)) {
            return value.plusWeeks(1);
        }
        if ("month".equals(trendUnit)) {
            return value.plusMonths(1).withDayOfMonth(1);
        }
        return value.plusDays(1);
    }

    private String formatTrendPeriod(LocalDate value, String trendUnit) {
        if ("week".equals(trendUnit)) {
            WeekFields weekFields = WeekFields.ISO;
            return String.format("%04d-%02d", value.get(weekFields.weekBasedYear()), value.get(weekFields.weekOfWeekBasedYear()));
        }
        if ("month".equals(trendUnit)) {
            return String.format("%04d-%02d", value.getYear(), value.getMonthValue());
        }
        return String.format("%04d-%02d-%02d", value.getYear(), value.getMonthValue(), value.getDayOfMonth());
    }

    private String formatTrendLabel(LocalDate value, String trendUnit) {
        if ("month".equals(trendUnit)) {
            return value.getYear() + "/" + String.format("%02d", value.getMonthValue());
        }
        return value.getMonthValue() + "/" + value.getDayOfMonth();
    }

    private long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
