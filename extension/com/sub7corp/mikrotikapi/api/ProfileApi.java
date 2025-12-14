package com.sub7corp.mikrotikapi.api;

import com.sub7corp.mikrotikapi.core.MkClient;

import java.io.IOException;

/**
 * Hotspot user profile API.
 * Used to create and manage time/traffic based profiles (vouchers).
 */
public class ProfileApi {

    private final MkClient client;

    public ProfileApi(MkClient client) {
        this.client = client;
    }

    /** /ip/hotspot/user/profile/print */
    public MkClient.MkResult listProfiles() throws IOException {
        return client.execute("/ip/hotspot/user/profile/print");
    }

    /**
     * Add a hotspot user profile
     *
     * @param name profile name
     * @param rateLimit e.g. "700k/700k"
     * @param sessionTimeout e.g. "1h", "30m", "1d"
     * @param sharedUsers number of shared users
     */
    public MkClient.MkResult addProfile(
            String name,
            String rateLimit,
            String sessionTimeout,
            int sharedUsers
    ) throws IOException {

        return client.execute(
                "/ip/hotspot/user/profile/add",
                "name=" + name,
                "rate-limit=" + rateLimit,
                "session-timeout=" + sessionTimeout,
                "shared-users=" + sharedUsers
        );
    }

    /**
     * Update an existing profile
     */
    public MkClient.MkResult setProfile(
            String name,
            String rateLimit,
            String sessionTimeout,
            int sharedUsers
    ) throws IOException {

        return client.execute(
                "/ip/hotspot/user/profile/set",
                "numbers=" + name,
                "rate-limit=" + rateLimit,
                "session-timeout=" + sessionTimeout,
                "shared-users=" + sharedUsers
        );
    }

    /**
     * Remove profile by name
     */
    public MkClient.MkResult removeProfile(String name) throws IOException {
        return client.execute(
                "/ip/hotspot/user/profile/remove",
                "numbers=" + name
        );
    }
}
