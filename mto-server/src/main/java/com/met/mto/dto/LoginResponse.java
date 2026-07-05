package com.met.mto.dto;

import java.util.List;
import lombok.Data;

@Data
public class LoginResponse {

    private String token;

    private long expiresIn;

    private Long userId;

    private String username;

    private String realName;

    private String role;

    private List<String> permissions;
}
