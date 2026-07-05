package com.met.mto.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.met.mto.entity.SysPermission;
import com.met.mto.entity.SysRolePermission;
import com.met.mto.entity.SysUserPermission;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.SysPermissionMapper;
import com.met.mto.mapper.SysRolePermissionMapper;
import com.met.mto.mapper.SysUserPermissionMapper;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final String EFFECT_ALLOW = "allow";
    private static final String EFFECT_DENY = "deny";

    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserPermissionMapper sysUserPermissionMapper;

    public void check(Long userId, String role, String... permissions) {
        if (hasAny(userId, role, permissions)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    public boolean hasAny(Long userId, String role, String... permissions) {
        if (!StringUtils.hasText(role) || permissions == null || permissions.length == 0) {
            return false;
        }
        Set<String> activePermissions = findActivePermissions();
        Set<String> deniedPermissions = findUserPermissions(userId, EFFECT_DENY, activePermissions);
        Set<String> rolePermissions = findRolePermissions(role, activePermissions);
        Set<String> allowedUserPermissions = findUserPermissions(userId, EFFECT_ALLOW, activePermissions);
        for (String permission : permissions) {
            if (!activePermissions.contains(permission)) {
                continue;
            }
            if (deniedPermissions.contains(permission)) {
                continue;
            }
            if (rolePermissions.contains(permission) || allowedUserPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> listEffectivePermissions(Long userId, String role) {
        if (!StringUtils.hasText(role)) {
            return Collections.emptySet();
        }
        Set<String> activePermissions = findActivePermissions();
        Set<String> deniedPermissions = findUserPermissions(userId, EFFECT_DENY, activePermissions);
        Set<String> rolePermissions = findRolePermissions(role, activePermissions);
        Set<String> allowedUserPermissions = findUserPermissions(userId, EFFECT_ALLOW, activePermissions);
        rolePermissions.addAll(allowedUserPermissions);
        rolePermissions.removeAll(deniedPermissions);
        return rolePermissions;
    }

    private Set<String> findActivePermissions() {
        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getStatus, 1))
                .stream()
                .map(SysPermission::getCode)
                .collect(Collectors.toSet());
    }

    private Set<String> findRolePermissions(String role, Set<String> activePermissions) {
        return sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRole, role))
                .stream()
                .map(SysRolePermission::getPermissionCode)
                .filter(activePermissions::contains)
                .collect(Collectors.toSet());
    }

    private Set<String> findUserPermissions(Long userId, String effect, Set<String> activePermissions) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return sysUserPermissionMapper.selectList(new LambdaQueryWrapper<SysUserPermission>()
                        .eq(SysUserPermission::getUserId, userId)
                        .eq(SysUserPermission::getEffect, effect))
                .stream()
                .map(SysUserPermission::getPermissionCode)
                .filter(activePermissions::contains)
                .collect(Collectors.toSet());
    }
}
