package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.common.PageResult;
import com.met.mto.dto.ResetPasswordRequest;
import com.met.mto.dto.SysUserQuery;
import com.met.mto.dto.SysUserRequest;
import com.met.mto.dto.SysUserResponse;
import com.met.mto.dto.UserPermissionRequest;
import com.met.mto.dto.UserPermissionResponse;
import com.met.mto.security.PermissionCode;
import com.met.mto.security.RequirePermission;
import com.met.mto.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping
    @RequirePermission(PermissionCode.USER_LIST)
    public ApiResult<PageResult<SysUserResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        SysUserQuery query = new SysUserQuery();
        query.setKeyword(keyword);
        query.setRole(role);
        query.setStatus(status);
        query.setPage(page);
        query.setSize(size);
        return ApiResult.ok(sysUserService.page(query));
    }

    @GetMapping("/{id}")
    @RequirePermission(PermissionCode.USER_DETAIL)
    public ApiResult<SysUserResponse> get(@PathVariable Long id) {
        return ApiResult.ok(sysUserService.get(id));
    }

    @PostMapping
    @RequirePermission(PermissionCode.USER_CREATE)
    public ApiResult<SysUserResponse> create(@RequestBody SysUserRequest request) {
        return ApiResult.ok(sysUserService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(PermissionCode.USER_EDIT)
    public ApiResult<SysUserResponse> update(@PathVariable Long id, @RequestBody SysUserRequest request) {
        return ApiResult.ok(sysUserService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequirePermission(PermissionCode.USER_STATUS)
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        sysUserService.updateStatus(id, status);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/password")
    @RequirePermission(PermissionCode.USER_PASSWORD)
    public ApiResult<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        sysUserService.resetPassword(id, request);
        return ApiResult.ok();
    }

    @GetMapping("/{id}/permissions")
    @RequirePermission(PermissionCode.USER_PERMISSION)
    public ApiResult<UserPermissionResponse> getPermissions(@PathVariable Long id) {
        return ApiResult.ok(sysUserService.getPermissions(id));
    }

    @PutMapping("/{id}/permissions")
    @RequirePermission(PermissionCode.USER_PERMISSION)
    public ApiResult<UserPermissionResponse> updatePermissions(
            @PathVariable Long id,
            @RequestBody UserPermissionRequest request
    ) {
        return ApiResult.ok(sysUserService.updatePermissions(id, request));
    }
}
