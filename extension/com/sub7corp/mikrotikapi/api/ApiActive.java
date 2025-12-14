package com.sub7corp.mikrotikapi.api;

import com.sub7corp.mikrotikapi.core.MkClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Hotspot Active Sessions API
 * List, remove (kick), and manage active users
 */
public class ActiveApi {

    private final MkClient client;

    public ActiveApi(MkClient client) {
        this.client = client;
    }

    /** /ip/hotspot/active/print */
    public MkClient.MkResult listActive() throws IOException {
        return client.execute("/ip/hotspot/active/print");
    }

    /**
     * Remove active session by .id
     * /ip/hotspot/active/remove .id=*X
     */
    public MkClient.MkResult removeActiveById(String id) throws IOException {
        return client.execute("/ip/hotspot/active/remove", ".id=" + id);
    }

    /**
     * Find active session .id by username
     * Returns null if not found
     */
    public String findActiveIdByUser(String username) throws IOException {
        MkClient.MkResult res = listActive();
        List<HashMap<String, String>> records = res.getRecords();

        for (HashMap<String, String> r : records) {
            String user = r.get("user");
            if (user != null && user.equals(username)) {
                return r.get(".id");
            }
        }
        return null;
    }

    /**
     * Kick active session by username
     * (resolves .id internally)
     */
    public MkClient.MkResult kickUser(String username) throws IOException {
        String id = findActiveIdByUser(username);
        if (id == null || id.isEmpty()) {
            throw new IOException("Active session not found for user: " + username);
        }
        return removeActiveById(id);
    }
}
