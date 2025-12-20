package com.sub7corp.mikrotikapi.api;

import com.sub7corp.mikrotikapi.core.MkClient;
import com.sub7corp.mikrotikapi.model.ApiError;
import com.sub7corp.mikrotikapi.model.ApiResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HotspotApi {

    private final MkClient client;

    public HotspotApi(MkClient client) {
        this.client = client;
    }

    // =========================
    // CREATE HOTSPOT USER
    // =========================
    public ApiResult createUser(
            String username,
            String password,
            String profile,
            String comment
    ) {
        try {
            MkClient.MkResult res = client.execute(
                    "/ip/hotspot/user/add",
                    "=name=" + username,
                    "=password=" + password,
                    "=profile=" + profile,
                    "=comment=" + comment
            );

            if (res.isError()) {
                return ApiResult.error(
                        new ApiError("HOTSPOT_CREATE_ERROR", res.getMessage())
                );
            }

            return ApiResult.success("USER_CREATED");

        } catch (Exception e) {
            return ApiResult.error(
                    new ApiError("HOTSPOT_CREATE_EXCEPTION", e.getMessage())
            );
        }
    }

    // =========================
    // REMOVE USER
    // =========================
    public ApiResult removeUser(String username) {
        try {
            MkClient.MkResult res = client.execute(
                    "/ip/hotspot/user/remove",
                    "=.id=" + username
            );

            if (res.isError()) {
                return ApiResult.error(
                        new ApiError("HOTSPOT_REMOVE_ERROR", res.getMessage())
                );
            }

            return ApiResult.success("USER_REMOVED");

        } catch (Exception e) {
            return ApiResult.error(
                    new ApiError("HOTSPOT_REMOVE_EXCEPTION", e.getMessage())
            );
        }
    }

    // =========================
    // LIST USERS
    // =========================
    public ApiResult listUsers() {
        try {
            MkClient.MkResult res = client.execute(
                    "/ip/hotspot/user/print"
            );

            if (res.isError()) {
                return ApiResult.error(
                        new ApiError("HOTSPOT_LIST_ERROR", res.getMessage())
                );
            }

            List<HashMap<String, String>> users = res.getRecords();
            return ApiResult.success(users);

        } catch (Exception e) {
            return ApiResult.error(
                    new ApiError("HOTSPOT_LIST_EXCEPTION", e.getMessage())
            );
        }
    }

    // =========================
    // DISCONNECT ACTIVE USER
    // =========================
    public ApiResult disconnectUser(String username) {
        try {
            MkClient.MkResult res = client.execute(
                    "/ip/hotspot/active/remove",
                    "=.id=" + username
            );

            if (res.isError()) {
                return ApiResult.error(
                        new ApiError("HOTSPOT_DISCONNECT_ERROR", res.getMessage())
                );
            }

            return ApiResult.success("USER_DISCONNECTED");

        } catch (Exception e) {
            return ApiResult.error(
                    new ApiError("HOTSPOT_DISCONNECT_EXCEPTION", e.getMessage())
            );
        }
    }
}
