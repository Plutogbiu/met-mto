package com.met.mto.service;

import com.met.mto.common.PageResult;
import com.met.mto.dto.ResetPasswordRequest;
import com.met.mto.dto.SysUserQuery;
import com.met.mto.dto.SysUserRequest;
import com.met.mto.dto.SysUserResponse;
import com.met.mto.dto.UserPermissionRequest;
import com.met.mto.dto.UserPermissionResponse;

public interface SysUserService {

    PageResult<SysUserResponse> page(SysUserQuery query);

    SysUserResponse get(Long id);

    SysUserResponse create(SysUserRequest request);

    SysUserResponse update(Long id, SysUserRequest request);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, ResetPasswordRequest request);

    UserPermissionResponse getPermissions(Long id);

    UserPermissionResponse updatePermissions(Long id, UserPermissionRequest request);
}
