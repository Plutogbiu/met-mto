package com.met.mto.dto;

import lombok.Data;

@Data
public class CustomerDeviceRequest {

    private String name;

    private String model;

    private String serialNo;

    private Integer status;
}
