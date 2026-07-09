package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.dto.WorkOrderRecordResponse;
import com.met.mto.security.PermissionCode;
import com.met.mto.security.RequirePermission;
import com.met.mto.service.WorkOrderRecordService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/work-orders/{workOrderId}/records")
@RequiredArgsConstructor
public class WorkOrderRecordController {

    private final WorkOrderRecordService workOrderRecordService;

    @GetMapping
    @RequirePermission(PermissionCode.WORK_ORDER_DETAIL)
    public ApiResult<List<WorkOrderRecordResponse>> list(
            @PathVariable Long workOrderId,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        return ApiResult.ok(workOrderRecordService.list(workOrderId, currentUserId, currentRole));
    }

    @PostMapping
    @RequirePermission(PermissionCode.WORK_ORDER_PROCESS)
    public ApiResult<WorkOrderRecordResponse> createProcessRecord(
            @PathVariable Long workOrderId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) String locationAddress,
            @RequestAttribute(value = "currentUserId", required = false) Long operatorId,
            @RequestAttribute(value = "currentRealName", required = false) String operatorName,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        return ApiResult.ok(workOrderRecordService.createProcessRecord(
                workOrderId,
                content,
                longitude,
                latitude,
                locationAddress,
                operatorId,
                operatorName,
                currentRole
        ));
    }
}
