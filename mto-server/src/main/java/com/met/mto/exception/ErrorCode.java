package com.met.mto.exception;

public enum ErrorCode {
    SUCCESS(0, "ok"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限操作"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统异常，请稍后重试"),
    BUSINESS_ERROR(1000, "业务处理失败"),
    LOGIN_PARAM_REQUIRED(1001, "请输入账号和密码"),
    LOGIN_FAILED(1002, "账号或密码错误"),
    CUSTOMER_SITE_NOT_FOUND(1101, "客户现场不存在"),
    CUSTOMER_SITE_NAME_REQUIRED(1102, "客户名称不能为空"),
    MAP_KEY_NOT_CONFIGURED(1201, "腾讯地图 Key 未配置"),
    MAP_REQUEST_FAILED(1202, "腾讯地图地址解析失败"),
    USER_NOT_FOUND(1301, "用户不存在"),
    USER_USERNAME_REQUIRED(1302, "账号不能为空"),
    USER_USERNAME_EXISTS(1303, "账号已存在"),
    USER_PASSWORD_REQUIRED(1304, "密码不能为空"),
    USER_ROLE_REQUIRED(1305, "角色不能为空"),
    USER_OLD_PASSWORD_INCORRECT(1306, "旧密码不正确"),
    WORK_ORDER_NOT_FOUND(1401, "工单不存在"),
    WORK_ORDER_CUSTOMER_REQUIRED(1403, "请选择客户"),
    WORK_ORDER_ENGINEER_REQUIRED(1404, "请选择现场实施工程师"),
    WORK_ORDER_DEVICE_REQUIRED(1405, "请选择设备"),
    WORK_ORDER_ARRIVAL_TIME_REQUIRED(1406, "请设置预计到达时间"),
    WORK_ORDER_COMPLETE_TIME_REQUIRED(1407, "请设置预估完成时间"),
    WORK_ORDER_CHECKIN_REQUIRED(1408, "请先完成现场打卡"),
    CUSTOMER_DEVICE_NOT_FOUND(1501, "设备不存在"),
    CUSTOMER_DEVICE_NAME_REQUIRED(1502, "设备名称不能为空"),
    ATTACHMENT_NOT_FOUND(1601, "附件不存在"),
    FILE_EMPTY(1602, "上传文件不能为空"),
    FILE_UPLOAD_FAILED(1603, "文件上传失败");

    
    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
