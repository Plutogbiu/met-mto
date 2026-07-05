package com.met.mto.common;

import com.met.mto.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResult<T> {

    private int code;

    private String message;

    private T data;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), null);
    }

    public static ApiResult<Void> fail(String message) {
        return new ApiResult<>(ErrorCode.BUSINESS_ERROR.code(), message, null);
    }

    public static ApiResult<Void> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public static ApiResult<Void> fail(ErrorCode errorCode) {
        return new ApiResult<>(errorCode.code(), errorCode.message(), null);
    }

    public static ApiResult<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResult<>(errorCode.code(), message, null);
    }
}
