package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.met.mto.dto.ChangePasswordRequest;
import com.met.mto.dto.LoginRequest;
import com.met.mto.dto.LoginResponse;
import com.met.mto.entity.SysUser;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.SysUserMapper;
import com.met.mto.security.PermissionService;
import com.met.mto.service.AuthService;
import com.met.mto.util.JwtTokenUtil;
import com.met.mto.util.PasswordUtil;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_PREFIX = "mto:auth:token:";
    private static final String CLIENT_ADMIN = "admin";
    private static final String CLIENT_APP = "app";
    private static final String ROLE_FIELD_ENGINEER = "field_engineer";

    private final SysUserMapper sysUserMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate redisTemplate;
    private final PermissionService permissionService;

    @Value("${mto.auth.admin-token-expire-minutes}")
    private long adminTokenExpireMinutes;

    @Value("${mto.auth.app-token-expire-days}")
    private long appTokenExpireDays;

    @Override
    public LoginResponse login(LoginRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_PARAM_REQUIRED);
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String passwordHash = PasswordUtil.sha256(request.getPassword());
        if (!passwordHash.equalsIgnoreCase(user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String clientType = resolveClientType(request.getClientType());
        if (CLIENT_ADMIN.equals(clientType) && ROLE_FIELD_ENGINEER.equals(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "现场实施人员只能登录 App");
        }

        Duration expires = resolveTokenExpires(clientType);
        String token = jwtTokenUtil.createToken(user.getId(), user.getUsername(), user.getRealName(), user.getRole(), clientType, expires);
        redisTemplate.opsForValue().set(tokenKey(token), String.valueOf(user.getId()), expires.getSeconds(), TimeUnit.SECONDS);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(expires.getSeconds());
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setPermissions(permissionService.listEffectivePermissions(user.getId(), user.getRole()).stream().sorted().collect(Collectors.toList()));
        return response;
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (request == null
                || !StringUtils.hasText(request.getOldPassword())
                || !StringUtils.hasText(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_REQUIRED);
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!PasswordUtil.sha256(request.getOldPassword()).equalsIgnoreCase(user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.USER_OLD_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(PasswordUtil.sha256(request.getNewPassword()));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            redisTemplate.delete(tokenKey(token));
        }
    }

    private String tokenKey(String token) {
        return TOKEN_PREFIX + token;
    }

    private Duration resolveTokenExpires(String clientType) {
        if (CLIENT_APP.equals(clientType)) {
            return Duration.ofDays(appTokenExpireDays);
        }
        return Duration.ofMinutes(adminTokenExpireMinutes);
    }

    private String resolveClientType(String clientType) {
        if (CLIENT_APP.equals(clientType)) {
            return CLIENT_APP;
        }
        return CLIENT_ADMIN;
    }
}
