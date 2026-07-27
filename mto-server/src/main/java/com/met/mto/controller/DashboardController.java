package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.dto.DashboardOverviewResponse;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.security.PermissionCode;
import com.met.mto.security.RequirePermission;
import com.met.mto.service.DashboardService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @RequirePermission(PermissionCode.DASHBOARD_VIEW)
    public ApiResult<DashboardOverviewResponse> overview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type
    ) {
        LocalDate today = LocalDate.now();
        LocalDate start = startDate == null || startDate.trim().isEmpty()
                ? today.withDayOfMonth(1) : LocalDate.parse(startDate);
        LocalDate end = endDate == null || endDate.trim().isEmpty() ? today : LocalDate.parse(endDate);
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        if (type != null && !type.trim().isEmpty() && !"onsite".equals(type) && !"inspection".equals(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的工单类型");
        }
        return ApiResult.ok(dashboardService.getOverview(start, end, type));
    }
}
