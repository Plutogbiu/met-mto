package com.met.mto.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;

    private String password;

    private String clientType;
}
