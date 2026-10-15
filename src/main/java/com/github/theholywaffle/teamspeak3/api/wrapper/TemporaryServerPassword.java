package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/** An active temporary server password. Secret getters return clear text; {@link #toString()} redacts it. */
public class TemporaryServerPassword extends Wrapper {
    public TemporaryServerPassword(Map<String, String> map) {
        super(map);
    }

    /** @return the clear-text temporary server password */
    public String getPassword() {
        return require("pw_clear");
    }

    public String getDescription() {
        return require("desc");
    }

    public String getCreatorNickname() {
        return require("nickname");
    }

    public String getCreatorUniqueId() {
        return require("uid");
    }

    /** @return when the password becomes valid, converted from the server's epoch seconds */
    public Instant getStartsAt() {
        return requireInstant("start");
    }

    /** @return when the password expires, converted from the server's epoch seconds */
    public Instant getExpiresAt() {
        return requireInstant("end");
    }

    /** @return the channel to join, or empty for the default channel */
    public Optional<ChannelId> getTargetChannelId() {
        return requireOptionalId("tcid", ChannelId::of);
    }

    /** @return the clear-text target channel password, or empty if none is set */
    public Optional<String> getTargetChannelPassword() {
        return findText("tcpw");
    }
}
