package com.met.mto.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SysUserResponse {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String role;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
