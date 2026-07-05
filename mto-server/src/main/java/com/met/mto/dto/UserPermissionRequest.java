package com.met.mto.dto;

import java.util.List;
import lombok.Data;

@Data
public class UserPermissionRequest {

    private List<UserPermissionItemRequest> permissions;
}
