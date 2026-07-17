package com.met.mto.service;

import com.met.mto.dto.ChangePasswordRequest;
import com.met.mto.dto.LoginRequest;
import com.met.mto.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);

    void logout(String token);
}
