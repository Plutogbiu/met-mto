package com.met.mto.dto;

import java.util.List;
import lombok.Data;

@Data
public class UserPermissionResponse {

    private Long userId;

    private String username;

    private String realName;

    private String role;

    private List<PermissionItemResponse> permissions;
}
