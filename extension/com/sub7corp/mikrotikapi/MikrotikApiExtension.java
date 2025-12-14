package com.sub7corp.mikrotikapi;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;

import com.sub7corp.mikrotikapi.core.MkClient;
import com.sub7corp.mikrotikapi.core.MkConnection;
import com.sub7corp.mikrotikapi.util.ThreadUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * MikroTik API Extension (RouterOS 6/7)
 * - Blocks defined via annotations in this single class.
 * - Uses your backend: MkConnection + MkClient (+ utils).
 */
@DesignerComponent(
        version = 1,
        description = "MikroTik API Extension (RouterOS 6/7). Connect (plain/SSL), execute commands, hotspot helpers, events.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/extension.png"
)
@SimpleObject(external = true)
public class MikrotikApiExtension extends AndroidNonvisibleComponent {

    // ====== Runtime ======
    private final Form form;

    private MkConnection connection;
    private MkClient client;

    // ====== Properties (defaults) ======
    private String host = "";
    private int port = 8728;
    private int sslPort = 8729;
    private boolean useSsl = false;

    private String username = "";
    private String password = "";

    private int soTimeoutMs = 8000;
    private int connectTimeoutMs = 8000;

    // MkConnection has allowInsecureSSL (trust all)
    private boolean allowInsecureSSL = true;

    public MikrotikApiExtension(ComponentContainer container) {
        super(container.$form());
        this.form = container.$form();
    }

    // =========================================================
    // EVENTS
    // =========================================================

    @SimpleEvent(description = "Fires when login succeeds.")
    public void OnConnected(String host, int port, boolean ssl) {
        EventDispatcher.dispatchEvent(this, "OnConnected", host, port, ssl);
    }

    @SimpleEvent(description = "Fires when connection is closed.")
    public void OnDisconnected() {
        EventDispatcher.dispatchEvent(this, "OnDisconnected");
    }

    /**
     * Generic result event.
     * action: CONNECT, EXECUTE, HOTSPOT_CREATE, HOTSPOT_REMOVE, HOTSPOT_DISABLE, HOTSPOT_ENABLE, HOTSPOT_ACTIVE, HOTSPOT_USERS
     * ok: true/false
     * json: structured JSON string
     */
    @SimpleEvent(description = "Generic result event: action, ok, json.")
    public void OnResult(String action, boolean ok, String json) {
        EventDispatcher.dispatchEvent(this, "OnResult", action, ok, json);
    }

    @SimpleEvent(description = "Error event: code, message, details.")
    public void OnError(String code, String message, String details) {
        EventDispatcher.dispatchEvent(this, "OnError", code, message, details);
    }

    // =========================================================
    // PROPERTIES (get/set blocks)
    // =========================================================

    @SimpleProperty(description = "Router host/IP.")
    public String Host() { return host; }

    @SimpleProperty(description = "Router host/IP.")
    public void Host(String value) { host = value != null ? value.trim() : ""; }

    @SimpleProperty(description = "API port (plain). Usually 8728.")
    public int Port() { return port; }

    @SimpleProperty(description = "API port (plain). Usually 8728.")
    public void Port(int value) { port = value; }

    @SimpleProperty(description = "API SSL port. Usually 8729.")
    public int SslPort() { return sslPort; }

    @SimpleProperty(description = "API SSL port. Usually 8729.")
    public void SslPort(int value) { sslPort = value; }

    @SimpleProperty(description = "Use SSL connection (API-SSL).")
    public boolean UseSsl() { return useSsl; }

    @SimpleProperty(description = "Use SSL connection (API-SSL).")
    public void UseSsl(boolean value) { useSsl = value; }

    @SimpleProperty(description = "Username for API login.")
    public String Username() { return username; }

    @SimpleProperty(description = "Username for API login.")
    public void Username(String value) { username = value != null ? value : ""; }

    @SimpleProperty(description = "Password for API login.")
    public String Password() { return password; }

    @SimpleProperty(description = "Password for API login.")
    public void Password(String value) { password = value != null ? value : ""; }

    @SimpleProperty(description = "Socket read timeout (ms).")
    public int SoTimeoutMs() { return soTimeoutMs; }

    @SimpleProperty(description = "Socket read timeout (ms).")
    public void SoTimeoutMs(int value) { soTimeoutMs = Math.max(1000, value); }

    @SimpleProperty(description = "Connect timeout (ms).")
    public int ConnectTimeoutMs() { return connectTimeoutMs; }

    @SimpleProperty(description = "Connect timeout (ms).")
    public void ConnectTimeoutMs(int value) { connectTimeoutMs = Math.max(1000, value); }

    @SimpleProperty(description = "Allow insecure SSL (trust all / self-signed).")
    public boolean AllowInsecureSSL() { return allowInsecureSSL; }

    @SimpleProperty(description = "Allow insecure SSL (trust all / self-signed).")
    public void AllowInsecureSSL(boolean value) { allowInsecureSSL = value; }

    // =========================================================
    // INTERNAL HELPERS
    // =========================================================

    private int currentPort() {
        return useSsl ? sslPort : port;
    }

    private void ui(Runnable r) {
        ThreadUtils.runOnUi(form, r);
    }

    private boolean isReady() {
        return connection != null && connection.isConnected() && client != null;
    }

    private void fail(String action, String code, String msg, String details) {
        ui(() -> {
            OnError(code, msg, details);
            OnResult(action, false, "{\"ok\":false,\"code\":\"" + esc(code) + "\",\"message\":\"" + esc(msg) + "\",\"details\":\"" + esc(details) + "\"}");
        });
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String mkResultToJson(MkClient.MkResult r) {
        try {
            JSONObject o = new JSONObject();
            if (r == null) {
                o.put("ok", false);
                o.put("message", "null result");
                return o.toString();
            }
            o.put("ok", r.isSuccess() && !r.isError());
            o.put("success", r.isSuccess());
            o.put("error", r.isError());
            o.put("message", r.getMessage() != null ? r.getMessage() : "");

            JSONArray arr = new JSONArray();
            List<HashMap<String, String>> records = r.getRecords();
            if (records != null) {
                for (HashMap<String, String> rec : records) {
                    JSONObject row = new JSONObject();
                    if (rec != null) {
                        for (String k : rec.keySet()) {
                            row.put(k, rec.get(k));
                        }
                    }
                    arr.put(row);
                }
            }
            o.put("records", arr);
            return o.toString();
        } catch (Exception e) {
            return "{\"ok\":false,\"message\":\"serialize_failed\",\"details\":\"" + esc(e.toString()) + "\"}";
        }
    }

    private String firstIdFromResult(MkClient.MkResult r) {
        try {
            if (r == null || r.getRecords() == null || r.getRecords().isEmpty()) return null;
            HashMap<String, String> rec = r.getRecords().get(0);
            if (rec == null) return null;
            // MkClient.parseRecord uses keys like ".id"
            return rec.get(".id");
        } catch (Exception ignored) {
            return null;
        }
    }

    // =========================================================
    // CORE BLOCKS
    // =========================================================

    @SimpleFunction(description = "Connect using current properties (Host/User/Pass/UseSsl). Non-blocking.")
    public void Connect() {
        final String action = "CONNECT";
        ThreadUtils.runAsync(() -> {
            try {
                // Clean previous
                try {
                    if (connection != null && connection.isConnected()) connection.disconnect();
                } catch (Exception ignored) {}

                connection = new MkConnection();
                connection.setSoTimeoutMs(soTimeoutMs);
                connection.setConnectTimeoutMs(connectTimeoutMs);
                connection.setAllowInsecureSSL(allowInsecureSSL);

                int p = currentPort();
                connection.connect(host, p, useSsl);

                boolean ok = connection.login(username, password);
                if (!ok) {
                    fail(action, "LOGIN_FAILED", "Login failed (credentials or API permissions).", "");
                    return;
                }

                client = new MkClient(connection);

                ui(() -> {
                    OnConnected(host, p, useSsl);
                    OnResult(action, true, "{\"ok\":true,\"connected\":true,\"host\":\"" + esc(host) + "\",\"port\":" + p + ",\"ssl\":" + (useSsl ? "true" : "false") + "}");
                });

            } catch (Exception e) {
                fail(action, "CONNECT_FAILED", "Failed to connect/login.", e.toString());
            }
        });
    }

    @SimpleFunction(description = "Disconnect. Non-blocking.")
    public void Disconnect() {
        final String action = "DISCONNECT";
        ThreadUtils.runAsync(() -> {
            try {
                if (connection != null) {
                    try { connection.disconnect(); } catch (Exception ignored) {}
                }
                connection = null;
                client = null;

                ui(() -> {
                    OnDisconnected();
                    OnResult(action, true, "{\"ok\":true,\"disconnected\":true}");
                });
            } catch (Exception e) {
                fail(action, "DISCONNECT_FAILED", "Failed to disconnect.", e.toString());
            }
        });
    }

    @SimpleFunction(description = "Returns true if connected.")
    public boolean IsConnected() {
        try {
            return connection != null && connection.isConnected() && client != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Execute raw command with optional params:
     * - path example: "/system/identity/print"
     * - params examples: "name=foo", "profile=default", ".id=*A", "?name=foo"
     */
    @SimpleFunction(description = "Execute raw API command: path + optional params. Result in OnResult('EXECUTE',...). Non-blocking.")
    public void Execute(String path, String paramsCsv) {
        final String action = "EXECUTE";
        final String p = path != null ? path.trim() : "";
        final String[] params = splitCsv(paramsCsv);

        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                MkClient.MkResult r = client.execute(p, params);
                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "EXECUTE_FAILED", "Failed executing command.", e.toString());
            }
        });
    }

    // =========================================================
    // HOTSPOT LOGICAL BLOCKS
    // =========================================================

    @SimpleFunction(description = "Create Hotspot user (name, password, profile, limitUptime e.g. '1h', '30m'). Non-blocking. Result in OnResult('HOTSPOT_CREATE',...).")
    public void HotspotCreateUser(String name, String pass, String profile, String limitUptime) {
        final String action = "HOTSPOT_CREATE";
        final String u = name != null ? name.trim() : "";
        final String pw = pass != null ? pass : "";
        final String pr = profile != null ? profile.trim() : "";
        final String lu = limitUptime != null ? limitUptime.trim() : "";

        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                // /ip/hotspot/user/add =name=U =password=PW =profile=PR =limit-uptime=1h
                MkClient.MkResult r = client.execute(
                        "/ip/hotspot/user/add",
                        "name=" + u,
                        "password=" + pw,
                        (pr.isEmpty() ? null : "profile=" + pr),
                        (lu.isEmpty() ? null : "limit-uptime=" + lu)
                );

                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "HOTSPOT_CREATE_FAILED", "Failed creating hotspot user.", e.toString());
            }
        });
    }

    @SimpleFunction(description = "Remove Hotspot user by name (internally resolves .id). Non-blocking. Result in OnResult('HOTSPOT_REMOVE',...).")
    public void HotspotRemoveUser(String name) {
        final String action = "HOTSPOT_REMOVE";
        final String u = name != null ? name.trim() : "";

        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                // find id
                MkClient.MkResult pr = client.execute("/ip/hotspot/user/print", "?name=" + u);
                String id = firstIdFromResult(pr);
                if (id == null || id.isEmpty()) {
                    ui(() -> OnResult(action, false, "{\"ok\":false,\"message\":\"user_not_found\",\"user\":\"" + esc(u) + "\"}"));
                    return;
                }

                MkClient.MkResult r = client.execute("/ip/hotspot/user/remove", ".id=" + id);
                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "HOTSPOT_REMOVE_FAILED", "Failed removing hotspot user.", e.toString());
            }
        });
    }

    @SimpleFunction(description = "Disable Hotspot user by name (internally resolves .id). Non-blocking. Result in OnResult('HOTSPOT_DISABLE',...).")
    public void HotspotDisableUser(String name) {
        final String action = "HOTSPOT_DISABLE";
        final String u = name != null ? name.trim() : "";

        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                MkClient.MkResult pr = client.execute("/ip/hotspot/user/print", "?name=" + u);
                String id = firstIdFromResult(pr);
                if (id == null || id.isEmpty()) {
                    ui(() -> OnResult(action, false, "{\"ok\":false,\"message\":\"user_not_found\",\"user\":\"" + esc(u) + "\"}"));
                    return;
                }

                MkClient.MkResult r = client.execute("/ip/hotspot/user/disable", ".id=" + id);
                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "HOTSPOT_DISABLE_FAILED", "Failed disabling hotspot user.", e.toString());
            }
        });
    }

    @SimpleFunction(description = "Enable Hotspot user by name (internally resolves .id). Non-blocking. Result in OnResult('HOTSPOT_ENABLE',...).")
    public void HotspotEnableUser(String name) {
        final String action = "HOTSPOT_ENABLE";
        final String u = name != null ? name.trim() : "";

        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                MkClient.MkResult pr = client.execute("/ip/hotspot/user/print", "?name=" + u);
                String id = firstIdFromResult(pr);
                if (id == null || id.isEmpty()) {
                    ui(() -> OnResult(action, false, "{\"ok\":false,\"message\":\"user_not_found\",\"user\":\"" + esc(u) + "\"}"));
                    return;
                }

                MkClient.MkResult r = client.execute("/ip/hotspot/user/enable", ".id=" + id);
                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "HOTSPOT_ENABLE_FAILED", "Failed enabling hotspot user.", e.toString());
            }
        });
    }

    @SimpleFunction(description = "List active hotspot sessions. Non-blocking. Result in OnResult('HOTSPOT_ACTIVE',...).")
    public void HotspotActive() {
        final String action = "HOTSPOT_ACTIVE";
        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                MkClient.MkResult r = client.execute("/ip/hotspot/active/print");
                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "HOTSPOT_ACTIVE_FAILED", "Failed listing active hotspot sessions.", e.toString());
            }
        });
    }

    @SimpleFunction(description = "List hotspot users. Non-blocking. Result in OnResult('HOTSPOT_USERS',...).")
    public void HotspotUsers() {
        final String action = "HOTSPOT_USERS";
        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                MkClient.MkResult r = client.execute("/ip/hotspot/user/print");
                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "HOTSPOT_USERS_FAILED", "Failed listing hotspot users.", e.toString());
            }
        });
    }

    // =========================================================
    // SYSTEM LOGICAL BLOCKS
    // =========================================================

    @SimpleFunction(description = "Get system identity. Non-blocking. Result in OnResult('SYSTEM_IDENTITY',...).")
    public void SystemIdentity() {
        final String action = "SYSTEM_IDENTITY";
        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                MkClient.MkResult r = client.execute("/system/identity/print");
                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "SYSTEM_IDENTITY_FAILED", "Failed reading identity.", e.toString());
            }
        });
    }

    @SimpleFunction(description = "Get system resource. Non-blocking. Result in OnResult('SYSTEM_RESOURCE',...).")
    public void SystemResource() {
        final String action = "SYSTEM_RESOURCE";
        ThreadUtils.runAsync(() -> {
            if (!isReady()) {
                fail(action, "NOT_CONNECTED", "Not connected. Call Connect first.", "");
                return;
            }
            try {
                MkClient.MkResult r = client.execute("/system/resource/print");
                final String json = mkResultToJson(r);
                ui(() -> OnResult(action, r != null && r.isSuccess() && !r.isError(), json));
            } catch (Exception e) {
                fail(action, "SYSTEM_RESOURCE_FAILED", "Failed reading resource.", e.toString());
            }
        });
    }

    // =========================================================
    // CSV PARAM PARSER (for Execute block)
    // =========================================================

    private String[] splitCsv(String csv) {
        if (csv == null) return new String[0];
        String s = csv.trim();
        if (s.isEmpty()) return new String[0];

        // Simple split by comma, trimming spaces
        String[] parts = s.split(",");
        int count = 0;
        for (String p : parts) {
            if (p != null && !p.trim().isEmpty()) count++;
        }
        String[] out = new String[count];
        int i = 0;
        for (String p : parts) {
            if (p == null) continue;
            p = p.trim();
            if (p.isEmpty()) continue;
            out[i++] = p;
        }
        return out;
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    public void onDelete() {
        try {
            try {
                if (connection != null && connection.isConnected()) {
                    connection.disconnect();
                }
            } catch (Exception ignored) {}
            connection = null;
            client = null;
        } catch (Exception ignored) {}
        super.onDelete();
    }
}
