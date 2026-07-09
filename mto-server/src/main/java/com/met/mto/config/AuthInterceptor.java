package com.met.mto.config;

import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.entity.SysUser;
import com.met.mto.mapper.SysUserMapper;
import com.met.mto.security.PermissionService;
import com.met.mto.security.RequirePermission;
import com.met.mto.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_PREFIX = "mto:auth:token:";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate redisTemplate;
    private final PermissionService permissionService;
    private final SysUserMapper sysUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        try {
            Claims claims = jwtTokenUtil.parseToken(token);
            Long currentUserId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String realName = claims.get("realName", String.class);
            if (!StringUtils.hasText(realName)) {
                realName = findRealName(currentUserId, username);
            }
            request.setAttribute("currentUserId", currentUserId);
            request.setAttribute("currentUsername", username);
            request.setAttribute("currentRealName", realName);
            request.setAttribute("currentRole", claims.get("role", String.class));
            request.setAttribute("currentClientType", claims.get("clientType", String.class));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Boolean exists = redisTemplate.hasKey(TOKEN_PREFIX + token);
        if (!Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        checkPermission(request, handler);
        return true;
    }

    private void checkPermission(HttpServletRequest request, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequirePermission permission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permission == null) {
            permission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (permission == null) {
            return;
        }
        permissionService.check(
                (Long) request.getAttribute("currentUserId"),
                (String) request.getAttribute("currentRole"),
                permission.value()
        );
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return request.getHeader("X-Token");
    }

    private String findRealName(Long userId, String username) {
        if (userId == null) {
            return username;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null && StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return username;
    }
}
