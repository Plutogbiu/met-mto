package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.dto.LoginRequest;
import com.met.mto.dto.LoginResponse;
import com.met.mto.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String CLIENT_ADMIN = "admin";

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        request.setClientType(CLIENT_ADMIN);
        return ApiResult.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length())
                : authorization;
        authService.logout(token);
        return ApiResult.ok();
    }
}
