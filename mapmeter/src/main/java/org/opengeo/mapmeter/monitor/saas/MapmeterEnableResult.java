package org.opengeo.mapmeter.monitor.saas;

import com.google.common.base.Objects;

public class MapmeterEnableResult {

    private final String serverApiKey;

    private final String username;

    private final String password;

    private final String externalUserId;

    private final String orgName;

    public MapmeterEnableResult(String serverApiKey, String username, String password,
            String externalUserId, String orgName) {
        this.serverApiKey = serverApiKey;
        this.username = username;
        this.password = password;
        this.externalUserId = externalUserId;
        this.orgName = orgName;
    }

    public String getServerApiKey() {
        return serverApiKey;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getOrgName() {
        return orgName;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(MapmeterEnableResult.class)
                .add("serverApiKey", serverApiKey)
                .add("username", username)
                .add("password", password)
                .add("externalUserId", externalUserId)
                .add("orgName", orgName)
                .toString();
    }

}
