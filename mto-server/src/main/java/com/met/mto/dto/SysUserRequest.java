package com.met.mto.dto;

import lombok.Data;

@Data
public class SysUserRequest {

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String role;

    private Integer status;
}
