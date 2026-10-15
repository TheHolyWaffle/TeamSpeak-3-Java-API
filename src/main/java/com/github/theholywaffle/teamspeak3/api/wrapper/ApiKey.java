package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ApiKeyId;
import com.github.theholywaffle.teamspeak3.api.ApiKeyScope;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * A Web Query API key. Only the result of creating a key includes its secret; {@link #toString()}
 * redacts it.
 */
public class ApiKey extends Wrapper {

    public ApiKey(Map<String, String> map) {
        super(map);
    }

    public ApiKeyId getId() {
        return requireId("id", ApiKeyId::new);
    }

    /** @return the virtual server the key is bound to, or empty for the server instance */
    public Optional<VirtualServerId> getVirtualServerId() {
        return requireOptionalId("sid", VirtualServerId::new);
    }

    /** @return the client whose permissions the key's requests use */
    public ClientDatabaseId getOwner() {
        return requireDatabaseId("cldbid");
    }

    /** @return the scope, or {@link ApiKeyScope#UNKNOWN} for a scope this library does not know */
    public ApiKeyScope getScope() {
        return ApiKeyScope.fromName(require("scope"));
    }

    /** @return the key to send in the {@code x-api-key} header; present only right after creation */
    public Optional<String> getSecret() {
        return findText("apikey");
    }

    public Instant getCreated() {
        return requireInstant("created_at");
    }

    /** @return whether the key never expires */
    public boolean isUnlimited() {
        return require("time_left").equals("unlimited");
    }

    /** @return the remaining lifetime, or empty for a key that never expires */
    public Optional<Duration> getTimeLeft() {
        return isUnlimited() ? Optional.empty() : Optional.of(requireSeconds("time_left"));
    }

    /** @return the expiry time, or empty for a key that never expires */
    public Optional<Instant> getExpires() {
        return isUnlimited() ? Optional.empty() : Optional.of(requireInstant("expires_at"));
    }
}
