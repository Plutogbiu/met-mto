package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.common.PageResult;
import com.met.mto.dto.WorkOrderContentRequest;
import com.met.mto.dto.WorkOrderExportDownloadFile;
import com.met.mto.dto.WorkOrderExportDownloadTicketResponse;
import com.met.mto.dto.WorkOrderExportTaskRequest;
import com.met.mto.dto.WorkOrderExportTaskResponse;
import com.met.mto.dto.WorkOrderQuery;
import com.met.mto.dto.WorkOrderRequest;
import com.met.mto.dto.WorkOrderResponse;
import com.met.mto.dto.WorkOrderStatusSummaryResponse;
import com.met.mto.dto.WorkOrderReceiptFile;
import com.met.mto.security.PermissionCode;
import com.met.mto.security.RequirePermission;
import com.met.mto.service.WorkOrderService;
import com.met.mto.service.WorkOrderExportTaskService;
import com.met.mto.service.WorkOrderReceiptPdfService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/admin/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    private final WorkOrderReceiptPdfService workOrderReceiptPdfService;

    private final WorkOrderExportTaskService workOrderExportTaskService;

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
            @RequestParam(required = false) String completedStart,
            @RequestParam(required = false) String completedEnd,
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
        query.setCompletedStart(parseStartTime(completedStart));
        query.setCompletedEnd(parseEndTime(completedEnd));
        query.setPage(page);
        query.setSize(size);
        return ApiResult.ok(workOrderService.page(query));
    }

    @GetMapping("/status-summary")
    @RequirePermission(PermissionCode.WORK_ORDER_LIST)
    public ApiResult<WorkOrderStatusSummaryResponse> statusSummary(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String customerKeyword,
            @RequestParam(required = false) Long customerSiteId,
            @RequestParam(required = false) Long engineerId,
            @RequestParam(required = false) String createdStart,
            @RequestParam(required = false) String createdEnd,
            @RequestParam(required = false) String completedStart,
            @RequestParam(required = false) String completedEnd,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        WorkOrderQuery query = new WorkOrderQuery();
        query.setKeyword(keyword);
        query.setType(type);
        query.setCustomerKeyword(customerKeyword);
        query.setCustomerSiteId(customerSiteId);
        query.setEngineerId("field_engineer".equals(currentRole) ? currentUserId : engineerId);
        query.setCreatedStart(parseStartTime(createdStart));
        query.setCreatedEnd(parseEndTime(createdEnd));
        query.setCompletedStart(parseStartTime(completedStart));
        query.setCompletedEnd(parseEndTime(completedEnd));
        return ApiResult.ok(workOrderService.statusSummary(query));
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

    @GetMapping("/{id}/receipt-pdf")
    @RequirePermission(PermissionCode.WORK_ORDER_RECEIPT_EXPORT)
    public ResponseEntity<byte[]> exportReceiptPdf(
            @PathVariable Long id,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        workOrderService.checkAccess(id, currentUserId, currentRole);
        WorkOrderReceiptFile file = workOrderReceiptPdfService.export(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(file.getContent().length);
        return ResponseEntity.ok().headers(headers).body(file.getContent());
    }

    @PostMapping("/export-tasks")
    @RequirePermission(PermissionCode.WORK_ORDER_RECEIPT_BATCH_EXPORT)
    public ApiResult<WorkOrderExportTaskResponse> createExportTask(
            @RequestBody WorkOrderExportTaskRequest request,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRealName", required = false) String currentRealName
    ) {
        return ApiResult.ok(workOrderExportTaskService.create(request, currentUserId, currentRealName));
    }

    @GetMapping("/export-tasks/{taskId}")
    @RequirePermission(PermissionCode.WORK_ORDER_RECEIPT_BATCH_EXPORT)
    public ApiResult<WorkOrderExportTaskResponse> getExportTask(
            @PathVariable Long taskId,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        return ApiResult.ok(workOrderExportTaskService.get(taskId, currentUserId, currentRole));
    }

    @PostMapping("/export-tasks/{taskId}/download-ticket")
    @RequirePermission(PermissionCode.WORK_ORDER_RECEIPT_BATCH_EXPORT)
    public ApiResult<WorkOrderExportDownloadTicketResponse> createExportTaskDownloadTicket(
            @PathVariable Long taskId,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        return ApiResult.ok(workOrderExportTaskService.createDownloadTicket(taskId, currentUserId, currentRole));
    }

    @GetMapping("/export-tasks/{taskId}/download")
    public ResponseEntity<StreamingResponseBody> downloadExportTask(
            @PathVariable Long taskId,
            @RequestParam String ticket
    ) {
        WorkOrderExportDownloadFile file = workOrderExportTaskService.consumeDownloadTicket(taskId, ticket);
        StreamingResponseBody body = output -> java.nio.file.Files.copy(file.getPath(), output);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.getFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(file.getFileSize());
        headers.setCacheControl("no-store");
        headers.add("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(headers).body(body);
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

    @PutMapping("/{id}/content")
    @RequirePermission(PermissionCode.WORK_ORDER_PROCESS)
    public ApiResult<Void> updateContent(
            @PathVariable Long id,
            @RequestBody WorkOrderContentRequest request,
            @RequestAttribute(value = "currentUserId", required = false) Long operatorId,
            @RequestAttribute(value = "currentRealName", required = false) String operatorName,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        workOrderService.checkAccess(id, operatorId, currentRole);
        workOrderService.updateContent(id, request == null ? null : request.getContent(), operatorId, operatorName);
        return ApiResult.ok();
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

    @PutMapping("/{id}/void")
    @RequirePermission(PermissionCode.WORK_ORDER_STATUS)
    public ApiResult<Void> voidOrder(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestAttribute(value = "currentUserId", required = false) Long operatorId,
            @RequestAttribute(value = "currentRealName", required = false) String operatorName,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        workOrderService.checkAccess(id, operatorId, currentRole);
        workOrderService.voidOrder(id, reason, operatorId, operatorName);
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

    @DeleteMapping("/{id}")
    @RequirePermission(PermissionCode.WORK_ORDER_DELETE)
    public ApiResult<Void> delete(@PathVariable Long id) {
        workOrderService.delete(id);
        return ApiResult.ok();
    }
}
