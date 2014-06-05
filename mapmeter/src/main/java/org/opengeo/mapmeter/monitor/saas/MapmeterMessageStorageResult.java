package org.opengeo.mapmeter.monitor.saas;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class MapmeterMessageStorageResult {

    private final String apiKey;

    private final boolean isValidApiKey;

    private final Optional<String> error;

    private MapmeterMessageStorageResult(String apiKey, boolean isValidApiKey,
            Optional<String> error) {
        this.apiKey = apiKey;
        this.isValidApiKey = isValidApiKey;
        this.error = error;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isValidApiKey() {
        return isValidApiKey;
    }

    public boolean isError() {
        return error.isPresent();
    }

    public String getError() {
        return error.get();
    }

    public static MapmeterMessageStorageResult invalidApiKey(String apiKey) {
        return new MapmeterMessageStorageResult(apiKey, false, Optional.<String> absent());
    }

    public static MapmeterMessageStorageResult error(String apiKey, String error) {
        return new MapmeterMessageStorageResult(apiKey, false, Optional.of(error));
    }

    public static MapmeterMessageStorageResult success(String apiKey) {
        return new MapmeterMessageStorageResult(apiKey, true, Optional.<String> absent());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(MapmeterMessageStorageResult.class)
                .add("apiKey", apiKey)
                .add("isValidApiKey", isValidApiKey)
                .add("error", error.orNull())
                .toString();
    }

}
