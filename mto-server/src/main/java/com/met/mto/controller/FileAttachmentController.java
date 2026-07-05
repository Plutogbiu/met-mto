package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.dto.FileAttachmentQuery;
import com.met.mto.dto.FileAttachmentResponse;
import com.met.mto.security.PermissionCode;
import com.met.mto.security.RequirePermission;
import com.met.mto.service.FileAttachmentService;
import com.met.mto.service.WorkOrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/attachments")
@RequiredArgsConstructor
public class FileAttachmentController {

    private final FileAttachmentService fileAttachmentService;
    private final WorkOrderService workOrderService;

    @PostMapping("/upload")
    @RequirePermission(PermissionCode.ATTACHMENT_UPLOAD)
    public ApiResult<FileAttachmentResponse> upload(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Long bizId,
            @RequestParam(required = false) String category,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        checkWorkOrderAccess(bizType, bizId, currentUserId, currentRole);
        return ApiResult.ok(fileAttachmentService.upload(file, bizType, bizId, category));
    }

    @GetMapping
    @RequirePermission(PermissionCode.ATTACHMENT_LIST)
    public ApiResult<List<FileAttachmentResponse>> list(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Long bizId,
            @RequestParam(required = false) String category,
            @RequestAttribute(value = "currentUserId", required = false) Long currentUserId,
            @RequestAttribute(value = "currentRole", required = false) String currentRole
    ) {
        checkWorkOrderAccess(bizType, bizId, currentUserId, currentRole);
        FileAttachmentQuery query = new FileAttachmentQuery();
        query.setBizType(bizType);
        query.setBizId(bizId);
        query.setCategory(category);
        return ApiResult.ok(fileAttachmentService.list(query));
    }

    @GetMapping("/{id}")
    @RequirePermission(PermissionCode.ATTACHMENT_DETAIL)
    public ApiResult<FileAttachmentResponse> get(@PathVariable Long id) {
        return ApiResult.ok(fileAttachmentService.get(id));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(PermissionCode.ATTACHMENT_DELETE)
    public ApiResult<Void> delete(@PathVariable Long id) {
        fileAttachmentService.delete(id);
        return ApiResult.ok();
    }

    private void checkWorkOrderAccess(String bizType, Long bizId, Long currentUserId, String currentRole) {
        if ("work_order".equals(bizType) && bizId != null) {
            workOrderService.checkAccess(bizId, currentUserId, currentRole);
        }
    }
}
