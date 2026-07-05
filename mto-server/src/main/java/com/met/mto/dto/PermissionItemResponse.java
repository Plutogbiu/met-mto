package com.met.mto.dto;

import lombok.Data;

@Data
public class PermissionItemResponse {

    private String code;

    private String name;

    private String module;

    private Integer status;

    private Boolean roleGranted;

    private String effect;

    private Boolean granted;
}
