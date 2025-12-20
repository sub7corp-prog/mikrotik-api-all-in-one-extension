package com.sub7corp.mikrotikapi.model;

public class ApiResult {

    private final boolean success;
    private final Object data;
    private final ApiError error;

    private ApiResult(boolean success, Object data, ApiError error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    // =========================
    // FACTORIES
    // =========================
    public static ApiResult success(Object data) {
        return new ApiResult(true, data, null);
    }

    public static ApiResult error(ApiError error) {
        return new ApiResult(false, null, error);
    }

    // =========================
    // GETTERS
    // =========================
    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }
}
