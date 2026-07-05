package com.met.mto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_user_permission")
public class SysUserPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String permissionCode;

    private String effect;

    private LocalDateTime createdAt;
}
