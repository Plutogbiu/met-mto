package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.met.mto.common.PageResult;
import com.met.mto.dto.PermissionItemResponse;
import com.met.mto.dto.ResetPasswordRequest;
import com.met.mto.dto.SysUserQuery;
import com.met.mto.dto.SysUserRequest;
import com.met.mto.dto.SysUserResponse;
import com.met.mto.dto.UserPermissionItemRequest;
import com.met.mto.dto.UserPermissionRequest;
import com.met.mto.dto.UserPermissionResponse;
import com.met.mto.entity.SysPermission;
import com.met.mto.entity.SysRolePermission;
import com.met.mto.entity.SysUser;
import com.met.mto.entity.SysUserPermission;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.SysPermissionMapper;
import com.met.mto.mapper.SysRolePermissionMapper;
import com.met.mto.mapper.SysUserMapper;
import com.met.mto.mapper.SysUserPermissionMapper;
import com.met.mto.service.SysUserService;
import com.met.mto.util.PasswordUtil;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private static final Set<String> ROLES = new HashSet<>(Arrays.asList("admin", "online_ops", "field_engineer"));
    private static final String EFFECT_ALLOW = "allow";
    private static final String EFFECT_DENY = "deny";
    private static final String EFFECT_INHERIT = "inherit";

    private final SysUserMapper sysUserMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysUserPermissionMapper sysUserPermissionMapper;

    @Override
    public PageResult<SysUserResponse> page(SysUserQuery query) {
        String keyword = query.getKeyword();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(StringUtils.hasText(keyword), item -> item
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getRealName, keyword)
                        .or()
                        .like(SysUser::getPhone, keyword))
                .eq(StringUtils.hasText(query.getRole()), SysUser::getRole, query.getRole())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getUpdatedAt)
                .orderByDesc(SysUser::getId);

        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(query.safePage(), query.safeSize()), wrapper);
        List<SysUserResponse> records = page.getRecords().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public SysUserResponse get(Long id) {
        SysUser user = findById(id);
        return toResponse(user);
    }

    @Override
    @Transactional
    public SysUserResponse create(SysUserRequest request) {
        checkRequest(request);
        checkUsername(request.getUsername());
        checkPassword(request.getPassword());
        checkRole(request.getRole());
        checkUsernameUnique(request.getUsername(), null);

        SysUser user = new SysUser();
        apply(user, request);
        user.setPasswordHash(PasswordUtil.sha256(request.getPassword()));
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(user.getCreatedAt());
        sysUserMapper.insert(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public SysUserResponse update(Long id, SysUserRequest request) {
        checkRequest(request);
        SysUser user = findById(id);
        checkUsername(request.getUsername());
        checkRole(request.getRole());
        checkUsernameUnique(request.getUsername(), id);

        apply(user, request);
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(PasswordUtil.sha256(request.getPassword()));
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysUser user = findById(id);
        user.setStatus(status == null ? 1 : status);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        SysUser user = findById(id);
        checkPassword(request.getPassword());
        user.setPasswordHash(PasswordUtil.sha256(request.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    public UserPermissionResponse getPermissions(Long id) {
        SysUser user = findById(id);
        return buildPermissionResponse(user);
    }

    @Override
    @Transactional
    public UserPermissionResponse updatePermissions(Long id, UserPermissionRequest request) {
        SysUser user = findById(id);
        sysUserPermissionMapper.delete(new LambdaQueryWrapper<SysUserPermission>()
                .eq(SysUserPermission::getUserId, id));
        if (request != null && request.getPermissions() != null) {
            Map<String, String> effects = normalizePermissionEffects(request.getPermissions());
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, String> entry : effects.entrySet()) {
                SysUserPermission userPermission = new SysUserPermission();
                userPermission.setUserId(id);
                userPermission.setPermissionCode(entry.getKey());
                userPermission.setEffect(entry.getValue());
                userPermission.setCreatedAt(now);
                sysUserPermissionMapper.insert(userPermission);
            }
        }
        return buildPermissionResponse(user);
    }

    private void apply(SysUser user, SysUserRequest request) {
        user.setUsername(request.getUsername().trim());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
    }

    private void checkRequest(SysUserRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private SysUser findById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserPermissionResponse buildPermissionResponse(SysUser user) {
        Set<String> rolePermissions = findRolePermissions(user.getRole());
        Map<String, String> userPermissionEffects = findUserPermissionEffects(user.getId());
        List<PermissionItemResponse> permissions = findPermissions().stream()
                .map(permission -> toPermissionItem(permission, rolePermissions, userPermissionEffects))
                .collect(Collectors.toList());

        UserPermissionResponse response = new UserPermissionResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setPermissions(permissions);
        return response;
    }

    private List<SysPermission> findPermissions() {
        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSortOrder)
                .orderByAsc(SysPermission::getId));
    }

    private Set<String> findRolePermissions(String role) {
        return sysRolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRole, role))
                .stream()
                .map(SysRolePermission::getPermissionCode)
                .collect(Collectors.toSet());
    }

    private Map<String, String> findUserPermissionEffects(Long userId) {
        return sysUserPermissionMapper.selectList(new LambdaQueryWrapper<SysUserPermission>()
                        .eq(SysUserPermission::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(
                        SysUserPermission::getPermissionCode,
                        SysUserPermission::getEffect,
                        (left, right) -> right
                ));
    }

    private PermissionItemResponse toPermissionItem(
            SysPermission permission,
            Set<String> rolePermissions,
            Map<String, String> userPermissionEffects
    ) {
        String code = permission.getCode();
        boolean roleGranted = rolePermissions.contains(code);
        String effect = userPermissionEffects.getOrDefault(code, EFFECT_INHERIT);
        boolean granted = permission.getStatus() != null
                && permission.getStatus() == 1
                && !EFFECT_DENY.equals(effect)
                && (roleGranted || EFFECT_ALLOW.equals(effect));

        PermissionItemResponse response = new PermissionItemResponse();
        response.setCode(code);
        response.setName(permission.getName());
        response.setModule(permission.getModule());
        response.setStatus(permission.getStatus());
        response.setRoleGranted(roleGranted);
        response.setEffect(effect);
        response.setGranted(granted);
        return response;
    }

    private Map<String, String> normalizePermissionEffects(List<UserPermissionItemRequest> permissions) {
        Set<String> permissionCodes = findPermissions().stream()
                .map(SysPermission::getCode)
                .collect(Collectors.toSet());
        Map<String, String> effects = new HashMap<>();
        for (UserPermissionItemRequest item : permissions) {
            if (item == null || !StringUtils.hasText(item.getCode()) || !StringUtils.hasText(item.getEffect())) {
                continue;
            }
            String code = item.getCode().trim();
            String effect = item.getEffect().trim();
            if (!permissionCodes.contains(code)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "权限编码不存在：" + code);
            }
            if (EFFECT_INHERIT.equals(effect)) {
                continue;
            }
            if (!EFFECT_ALLOW.equals(effect) && !EFFECT_DENY.equals(effect)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "权限效果只能是 allow 或 deny");
            }
            effects.put(code, effect);
        }
        return effects;
    }

    private void checkUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.USER_USERNAME_REQUIRED);
        }
    }

    private void checkPassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_REQUIRED);
        }
    }

    private void checkRole(String role) {
        if (!StringUtils.hasText(role) || !ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.USER_ROLE_REQUIRED);
        }
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USER_USERNAME_EXISTS);
        }
    }

    private SysUserResponse toResponse(SysUser user) {
        SysUserResponse response = new SysUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
