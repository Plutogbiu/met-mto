package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.common.PageResult;
import com.met.mto.dto.WorkOrderQuery;
import com.met.mto.dto.WorkOrderRequest;
import com.met.mto.dto.WorkOrderResponse;
import com.met.mto.security.PermissionCode;
import com.met.mto.security.RequirePermission;
import com.met.mto.service.WorkOrderService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    @RequirePermission(PermissionCode.WORK_ORDER_LIST)
    public ApiResult<PageResult<WorkOrderResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerKeyword,
            @RequestParam(required = false) Boolean unfinishedOnly,
            @RequestParam(required = false) Long customerSiteId,
            @RequestParam(required = false) Long engineerId,
            @RequestParam(required = false) String createdStart,
            @RequestParam(required = false) String createdEnd,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        WorkOrderQuery query = new WorkOrderQuery();
        query.setKeyword(keyword);
        query.setType(type);
        query.setStatus(status);
        query.setCustomerKeyword(customerKeyword);
        query.setUnfinishedOnly(unfinishedOnly);
        query.setCustomerSiteId(customerSiteId);
        query.setEngineerId("field_engineer".equals(currentRole) ? currentUserId : engineerId);
        query.setCreatedStart(parseStartTime(createdStart));
        query.setCreatedEnd(parseEndTime(createdEnd));
        query.setPage(page);
        query.setSize(size);
        return ApiResult.ok(workOrderService.page(query));
    }

    @GetMapping("/{id}")
    @RequirePermission(PermissionCode.WORK_ORDER_DETAIL)
    public ApiResult<WorkOrderResponse> get(
            @PathVariable Long id,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        workOrderService.checkAccess(id, currentUserId, currentRole);
        return ApiResult.ok(workOrderService.get(id));
    }

    private LocalDateTime parseStartTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay();
        }
        return LocalDateTime.parse(value);
    }

    private LocalDateTime parseEndTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        if (value.length() == 10) {
            return LocalDate.parse(value).atTime(23, 59, 59);
        }
        return LocalDateTime.parse(value);
    }

    @PostMapping
    @RequirePermission(PermissionCode.WORK_ORDER_CREATE)
    public ApiResult<WorkOrderResponse> create(
            @RequestBody WorkOrderRequest request,
            @RequestAttribute(value = "currentUserId", required = false) Long operatorId,
            @RequestAttribute(value = "currentRealName", required = false) String operatorName
    ) {
        return ApiResult.ok(workOrderService.create(request, operatorId, operatorName));
    }

    @PutMapping("/{id}")
    @RequirePermission(PermissionCode.WORK_ORDER_EDIT)
    public ApiResult<WorkOrderResponse> update(
            @PathVariable Long id,
            @RequestBody WorkOrderRequest request,
            @RequestAttribute(value = "currentUserId", required = false) Long operatorId,
            @RequestAttribute(value = "currentRealName", required = false) String operatorName,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        workOrderService.checkAccess(id, operatorId, currentRole);
        return ApiResult.ok(workOrderService.update(id, request, operatorId, operatorName));
    }

    @PutMapping("/{id}/status")
    @RequirePermission(PermissionCode.WORK_ORDER_STATUS)
    public ApiResult<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestAttribute(value = "currentUserId", required = false) Long operatorId,
            @RequestAttribute(value = "currentRealName", required = false) String operatorName,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        workOrderService.checkAccess(id, operatorId, currentRole);
        workOrderService.updateStatus(id, status, operatorId, operatorName);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/complete")
    @RequirePermission(PermissionCode.WORK_ORDER_COMPLETE)
    public ApiResult<Void> complete(
            @PathVariable Long id,
            @RequestAttribute(value = "currentUserId", required = false) Long operatorId,
            @RequestAttribute(value = "currentRealName", required = false) String operatorName,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        workOrderService.checkAccess(id, operatorId, currentRole);
        workOrderService.complete(id, operatorId, operatorName);
        return ApiResult.ok();
    }
}
