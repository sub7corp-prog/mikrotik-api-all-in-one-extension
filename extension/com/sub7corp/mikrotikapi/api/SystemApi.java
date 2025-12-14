package com.sub7corp.mikrotikapi.api;

import com.sub7corp.mikrotikapi.core.MkClient;
import com.sub7corp.mikrotikapi.core.MkResponse;
import com.sub7corp.mikrotikapi.model.ApiError;
import com.sub7corp.mikrotikapi.model.ApiResult;

import java.util.HashMap;
import java.util.Map;

public class SystemApi {

    private final MkClient client;

    public SystemApi(MkClient client) {
        this.client = client;
    }

    // =========================
    // SYSTEM IDENTITY
    // =========================
    public ApiResult getIdentity() {
        try {
            MkResponse res = client.execute(
                    "/system/identity/print"
            );

            if (res.hasTrap()) {
                return ApiResult.error(
                        new ApiError("SYSTEM_IDENTITY_ERROR", res.getTrapMessage())
                );
            }

            Map<String, String> data = new HashMap<>();
            if (!res.getRecords().isEmpty()) {
                data.putAll(res.getRecords().get(0));
            }

            return ApiResult.success(data);

        } catch (Exception e) {
            return ApiResult.error(
                    new ApiError("SYSTEM_IDENTITY_EXCEPTION", e.getMessage())
            );
        }
    }

    // =========================
    // SYSTEM RESOURCE
    // =========================
    public ApiResult getResources() {
        try {
            MkResponse res = client.execute(
                    "/system/resource/print"
            );

            if (res.hasTrap()) {
                return ApiResult.error(
                        new ApiError("SYSTEM_RESOURCE_ERROR", res.getTrapMessage())
                );
            }

            Map<String, String> data = new HashMap<>();
            if (!res.getRecords().isEmpty()) {
                data.putAll(res.getRecords().get(0));
            }

            return ApiResult.success(data);

        } catch (Exception e) {
            return ApiResult.error(
                    new ApiError("SYSTEM_RESOURCE_EXCEPTION", e.getMessage())
            );
        }
    }

    // =========================
    // SYSTEM CLOCK
    // =========================
    public ApiResult getClock() {
        try {
            MkResponse res = client.execute(
                    "/system/clock/print"
            );

            if (res.hasTrap()) {
                return ApiResult.error(
                        new ApiError("SYSTEM_CLOCK_ERROR", res.getTrapMessage())
                );
            }

            Map<String, String> data = new HashMap<>();
            if (!res.getRecords().isEmpty()) {
                data.putAll(res.getRecords().get(0));
            }

            return ApiResult.success(data);

        } catch (Exception e) {
            return ApiResult.error(
                    new ApiError("SYSTEM_CLOCK_EXCEPTION", e.getMessage())
            );
        }
    }

    // =========================
    // PING (HEALTH CHECK)
    // =========================
    public ApiResult ping(String address, int count) {
        try {
            MkResponse res = client.execute(
                    "/ping",
                    "=address=" + address,
                    "=count=" + count
            );

            if (res.hasTrap()) {
                return ApiResult.error(
                        new ApiError("PING_ERROR", res.getTrapMessage())
                );
            }

            return ApiResult.success(res.getRecords());

        } catch (Exception e) {
            return ApiResult.error(
                    new ApiError("PING_EXCEPTION", e.getMessage())
            );
        }
    }
}
