package com.met.mto.dto;

import lombok.Data;

@Data
public class AppVersionResponse {

    private Boolean needUpdate;

    private String platform;

    private String updateType;

    private String versionName;

    private Integer versionCode;

    private Integer minVersionCode;

    private Integer baseVersionCode;

    private Boolean forceUpdate;

    private String downloadUrl;

    private String releaseNotes;
}
