package com.sub7corp.mikrotikapi;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;

import com.sub7corp.mikrotikapi.api.HotspotApi;
import com.sub7corp.mikrotikapi.api.SystemApi;
import com.sub7corp.mikrotikapi.core.MkClient;
import com.sub7corp.mikrotikapi.model.ApiResult;

@DesignerComponent(
        version = 1,
        description = "MikroTik API Extension (RouterOS 6 / 7)",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "aiwebres/icon.png"
)
@SimpleObject(external = true)
public class MikrotikApiExtension extends AndroidNonvisibleComponent {

    private MkClient client;
    private HotspotApi hotspotApi;
    private SystemApi systemApi;

    private final ComponentContainer container;

    public MikrotikApiExtension(ComponentContainer container) {
        super(container.$form());
        this.container = container;
    }

    // =========================
    // CONNECT
    // =========================
    @SimpleFunction(description = "Connect to MikroTik using API")
    public void Connect(
            final String host,
            final int port,
            final String user,
            final String password
    ) {
        new Thread(() -> {
            try {
                client = new MkClient(host, port);
                client.connect(user, password);

                hotspotApi = new HotspotApi(client);
                systemApi = new SystemApi(client);

                container.$form().runOnUiThread(this::OnConnected);

            } catch (Exception e) {
                container.$form().runOnUiThread(() ->
                        OnError("CONNECT_ERROR", e.getMessage())
                );
            }
        }).start();
    }

    // =========================
    // DISCONNECT
    // =========================
    @SimpleFunction(description = "Disconnect from MikroTik")
    public void Disconnect() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    // =========================
    // SYSTEM BLOCKS
    // =========================
    @SimpleFunction(description = "Get system identity")
    public void GetIdentity() {
        runAsync(() -> systemApi.getIdentity());
    }

    @SimpleFunction(description = "Get system resources")
    public void GetResources() {
        runAsync(() -> systemApi.getResources());
    }

    // =========================
    // HOTSPOT BLOCKS
    // =========================
    @SimpleFunction(description = "Create Hotspot User")
    public void CreateHotspotUser(
            String username,
            String password,
            String profile,
            String comment
    ) {
        runAsync(() ->
                hotspotApi.createUser(username, password, profile, comment)
        );
    }

    @SimpleFunction(description = "List Hotspot Users")
    public void ListHotspotUsers() {
        runAsync(() ->
                hotspotApi.listUsers()
        );
    }

    @SimpleFunction(description = "Remove Hotspot User")
    public void RemoveHotspotUser(String id) {
        runAsync(() ->
                hotspotApi.removeUser(id)
        );
    }

    // =========================
    // ASYNC HANDLER
    // =========================
    private void runAsync(Task task) {
        new Thread(() -> {
            ApiResult result = task.run();
            container.$form().runOnUiThread(() -> {
                if (result.isSuccess()) {
                    OnSuccess(result.getData());
                } else {
                    OnError(result.getError().getCode(),
                            result.getError().getMessage());
                }
            });
        }).start();
    }

    private interface Task {
        ApiResult run();
    }

    // =========================
    // EVENTS
    // =========================
    @SimpleEvent
    public void OnConnected() {
        EventDispatcher.dispatchEvent(this, "OnConnected");
    }

    @SimpleEvent
    public void OnSuccess(Object data) {
        EventDispatcher.dispatchEvent(this, "OnSuccess", data);
    }

    @SimpleEvent
    public void OnError(String code, String message) {
        EventDispatcher.dispatchEvent(this, "OnError", code, message);
    }
}
