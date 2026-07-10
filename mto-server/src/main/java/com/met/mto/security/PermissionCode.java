package com.met.mto.security;

public final class PermissionCode {

    public static final String USER_LIST = "user:list";
    public static final String USER_DETAIL = "user:detail";
    public static final String USER_CREATE = "user:create";
    public static final String USER_EDIT = "user:edit";
    public static final String USER_STATUS = "user:status";
    public static final String USER_PASSWORD = "user:password";
    public static final String USER_PERMISSION = "user:permission";

    public static final String CUSTOMER_LIST = "customer:list";
    public static final String CUSTOMER_DETAIL = "customer:detail";
    public static final String CUSTOMER_CREATE = "customer:create";
    public static final String CUSTOMER_EDIT = "customer:edit";
    public static final String CUSTOMER_STATUS = "customer:status";

    public static final String DEVICE_LIST = "device:list";
    public static final String DEVICE_DETAIL = "device:detail";
    public static final String DEVICE_CREATE = "device:create";
    public static final String DEVICE_EDIT = "device:edit";
    public static final String DEVICE_STATUS = "device:status";

    public static final String WORK_ORDER_LIST = "work-order:list";
    public static final String WORK_ORDER_DETAIL = "work-order:detail";
    public static final String WORK_ORDER_CREATE = "work-order:create";
    public static final String WORK_ORDER_EDIT = "work-order:edit";
    public static final String WORK_ORDER_STATUS = "work-order:status";
    public static final String WORK_ORDER_COMPLETE = "work-order:complete";
    public static final String WORK_ORDER_PROCESS = "work-order:process";
    public static final String WORK_ORDER_DELETE = "work-order:delete";

    public static final String ATTACHMENT_LIST = "attachment:list";
    public static final String ATTACHMENT_DETAIL = "attachment:detail";
    public static final String ATTACHMENT_UPLOAD = "attachment:upload";
    public static final String ATTACHMENT_DELETE = "attachment:delete";

    private PermissionCode() {
    }
}
