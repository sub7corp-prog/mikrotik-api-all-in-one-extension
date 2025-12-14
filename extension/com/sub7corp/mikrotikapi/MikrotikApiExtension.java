package com.sub7corp.mikrotikapi;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;

import com.sub7corp.mikrotikapi.core.MkClient;
import com.sub7corp.mikrotikapi.core.MkConnection;
import com.sub7corp.mikrotikapi.core.MkResponse;

import com.sub7corp.mikrotikapi.api.HotspotApi;
import com.sub7corp.mikrotikapi.api.SystemApi;
import com.sub7corp.mikrotikapi.api.ProfileApi;
import com.sub7corp.mikrotikapi.api.ActiveApi;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLHandshakeException;

@DesignerComponent(
        version = 2,
        description = "All-in-One MikroTik API Extension (RouterOS 6/7+, SSL support) - threaded, safer",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/mikrotik.png"
)
@SimpleObject(external = true)
public class MikrotikApiExtension extends AndroidNonvisibleComponent {

    /* =========================
       ========== STATE =========
       ========================= */

    private String host = "";
    private int port = 8728;
    private boolean useSSL = false;
    private boolean allowInsecureSSL = false;

    private String username = "";
    private String password = "";

    private MkConnection connection;
    private MkClient client;

    private HotspotApi hotspotApi;
    private SystemApi systemApi;
    private ProfileApi profileApi;
    private ActiveApi activeApi;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /* =========================
       ===== CONSTRUCTOR =======
       ========================= */

    public MikrotikApiExtension(ComponentContainer container) {
        super(container.$form());
    }

    /* =========================
       ===== CONFIG PROPS =======
       ========================= */

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty(description = "MikroTik host or IP")
    public void Host(String value) { this.host = value; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "8728")
    @SimpleProperty(description = "MikroTik API port (8728 / 8729 SSL)")
    public void Port(int value) { this.port = value; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "false")
    @SimpleProperty(description = "Use SSL (RouterOS 7+)")
    public void UseSSL(boolean value) { this.useSSL = value; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "false")
    @SimpleProperty(description = "Allow insecure SSL (trust all certs). Use ONLY in LAN/dev.")
    public void AllowInsecureSSL(boolean value) { this.allowInsecureSSL = value; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty(description = "MikroTik username")
    public void Username(String value) { this.username = value; }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty(description = "MikroTik password")
    public void Password(String value) { this.password = value; }

    /* =========================
       ===== CONNECTION =========
       ========================= */

    @SimpleFunction(description = "Connect and login to MikroTik (runs in background)")
    public void Connect() {
        executor.execute(() -> {
            try {
                // Close existing connection if any
                if (connection != null && connection.isConnected()) {
                    connection.disconnect();
                }

                connection = new MkConnection();
                connection.setAllowInsecureSSL(allowInsecureSSL);
                connection.connect(host, port, useSSL);

                boolean logged = connection.login(username, password);
                if (!logged) {
                    postError("Login failed");
                    return;
                }

                client = new MkClient(connection);
                hotspotApi = new HotspotApi(client);
                systemApi = new SystemApi(client);
                profileApi = new ProfileApi(client);
                activeApi = new ActiveApi(client);

                postConnected();

            } catch (SSLHandshakeException e) {
                postError("SSL handshake failed: " + safeMsg(e));
            } catch (SocketTimeoutException e) {
                postError("Connection timeout");
            } catch (IOException e) {
                postError("I/O error: " + safeMsg(e));
            } catch (Exception e) {
                postError("Unexpected error: " + safeMsg(e));
            }
        });
    }

    @SimpleFunction(description = "Disconnect from MikroTik")
    public void Disconnect() {
        executor.execute(() -> {
            try {
                if (connection != null) connection.disconnect();
            } catch (Exception ignored) {}
            postDisconnected();
        });
    }

    @SimpleFunction(description = "Returns true if connected")
    public boolean IsConnected() {
        return connection != null && connection.isConnected();
    }

    /* =========================
       ===== HOTSPOT BLOCKS =====
       ========================= */

    @SimpleFunction(description = "List hotspot users (background)")
    public void HotspotListUsers() {
        runApi("hotspot_users", () -> hotspotApi.listUsers());
    }

    @SimpleFunction(description = "List active hotspot sessions (background)")
    public void HotspotListActive() {
        runApi("hotspot_active", () -> hotspotApi.listActive());
    }

    @SimpleFunction(description = "Add hotspot user (background)")
    public void HotspotAddUser(String name, String profile, String password, String comment) {
        runApi("hotspot_add_user", () -> hotspotApi.addUser(name, profile, password, comment));
    }

    @SimpleFunction(description = "Remove hotspot user by name (background)")
    public void HotspotRemoveUser(String name) {
        runApi("hotspot_remove_user", () -> hotspotApi.removeUserByName(name));
    }

    /* =========================
       ===== PROFILE BLOCKS =====
       ========================= */

    @SimpleFunction(description = "List hotspot profiles (background)")
    public void ProfileList() {
        runApi("profile_list", () -> profileApi.listProfiles());
    }

    @SimpleFunction(description = "Add hotspot profile (background)")
    public void ProfileAdd(String name, String rateLimit, String sessionTimeout, int sharedUsers) {
        runApi("profile_add", () -> profileApi.addProfile(name, rateLimit, sessionTimeout, sharedUsers));
    }

    @SimpleFunction(description = "Update hotspot profile (background)")
    public void ProfileSet(String name, String rateLimit, String sessionTimeout, int sharedUsers) {
        runApi("profile_set", () -> profileApi.setProfile(name, rateLimit, sessionTimeout, sharedUsers));
    }

    @SimpleFunction(description = "Remove hotspot profile (background)")
    public void ProfileRemove(String name) {
        runApi("profile_remove", () -> profileApi.removeProfile(name));
    }

    /* =========================
       ===== ACTIVE BLOCKS ======
       ========================= */

    @SimpleFunction(description = "List active sessions (background)")
    public void ActiveList() {
        runApi("active_list", () -> activeApi.listActive());
    }

    @SimpleFunction(description = "Kick active user by username (background)")
    public void ActiveKickUser(String username) {
        runApi("active_kick", () -> activeApi.kickUser(username));
    }

    /* =========================
       ===== SYSTEM BLOCKS ======
       ========================= */

    @SimpleFunction(description = "Get system identity (background)")
    public void SystemGetIdentity() {
        runApi("system_identity", () -> systemApi.getIdentity());
    }

    @SimpleFunction(description = "Get system resources (background)")
    public void SystemGetResources() {
        runApi("system_resources", () -> systemApi.getResources());
    }

    @SimpleFunction(description = "Get system clock (background)")
    public void SystemGetClock() {
        runApi("system_clock", () -> systemApi.getClock());
    }

    @SimpleFunction(description = "Get routerboard info (background)")
    public void SystemGetRouterBoard() {
        runApi("system_routerboard", () -> systemApi.getRouterBoard());
    }

    /* =========================
       ===== INTERNAL ASYNC =====
       ========================= */

    private interface ApiCall {
        MkClient.MkResult call() throws Exception;
    }

    private void runApi(String tag, ApiCall call) {
        executor.execute(() -> {
            if (!IsConnected()) {
                postError("Not connected");
                return;
            }
            try {
                MkClient.MkResult res = call.call();
                postResult(tag, MkResponse.toJson(res));
            } catch (SocketTimeoutException e) {
                postError("Timeout: " + tag);
            } catch (IOException e) {
                postError("I/O error (" + tag + "): " + safeMsg(e));
            } catch (Exception e) {
                postError("Error (" + tag + "): " + safeMsg(e));
            }
        });
    }

    private String safeMsg(Exception e) {
        return (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
    }

    private void postConnected() {
        form.runOnUiThread(this::OnConnected);
    }

    private void postDisconnected() {
        form.runOnUiThread(this::OnDisconnected);
    }

    private void postError(String msg) {
        form.runOnUiThread(() -> OnError(msg));
    }

    private void postResult(String tag, String json) {
        form.runOnUiThread(() -> OnResult(tag, json));
    }

    /* =========================
       ========= EVENTS =========
       ========================= */

    @SimpleEvent(description = "Connected to MikroTik")
    public void OnConnected() {
        EventDispatcher.dispatchEvent(this, "OnConnected");
    }

    @SimpleEvent(description = "Disconnected from MikroTik")
    public void OnDisconnected() {
        EventDispatcher.dispatchEvent(this, "OnDisconnected");
    }

    @SimpleEvent(description = "Error event")
    public void OnError(String message) {
        EventDispatcher.dispatchEvent(this, "OnError", message);
    }

    @SimpleEvent(description = "API result event (JSON)")
    public void OnResult(String tag, String json) {
        EventDispatcher.dispatchEvent(this, "OnResult", tag, json);
    }
}
