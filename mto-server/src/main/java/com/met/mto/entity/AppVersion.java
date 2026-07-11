package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("app_version")
public class AppVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String platform;

    private String updateType;

    private String versionName;

    private Integer versionCode;

    private Integer minVersionCode;

    private Integer baseVersionCode;

    private String downloadUrl;

    private String releaseNotes;

    private Integer forceUpdate;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
