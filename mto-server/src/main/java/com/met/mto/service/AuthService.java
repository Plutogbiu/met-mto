package com.met.mto.service;

import com.met.mto.dto.LoginRequest;
import com.met.mto.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String token);
}
