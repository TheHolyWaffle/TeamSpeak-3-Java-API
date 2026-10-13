package com.github.theholywaffle.teamspeak3.api;

import java.time.Duration;
import java.util.Objects;

/**
 * A ban rule. All given patterns must match a client. Start with one of the static factories,
 * then add further patterns, a duration or a reason.
 *
 * @param ip a regular expression matched against a client's IP, or {@code null}
 * @param name a regular expression matched against a client's nickname, or {@code null}
 * @param uniqueId a client unique identifier, or {@code null}
 * @param myTeamSpeakId a myTeamSpeak ID, or {@code null}; {@code "empty"} matches clients without one
 * @param duration the ban duration; {@link Duration#ZERO} is permanent
 * @param reason the ban reason, or {@code null}
 */
public record BanRequest(
        String ip, String name, String uniqueId, String myTeamSpeakId, Duration duration, String reason) {

    public BanRequest {
        if (ip == null && name == null && uniqueId == null && myTeamSpeakId == null) {
            throw new IllegalArgumentException("A ban needs an IP, name, unique ID or myTeamSpeak ID pattern");
        }
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) throw new IllegalArgumentException("Duration must not be negative");
    }

    public static BanRequest ip(String ipPattern) {
        return new BanRequest(Objects.requireNonNull(ipPattern), null, null, null, Duration.ZERO, null);
    }

    public static BanRequest name(String namePattern) {
        return new BanRequest(null, Objects.requireNonNull(namePattern), null, null, Duration.ZERO, null);
    }

    public static BanRequest uniqueId(String uniqueId) {
        return new BanRequest(null, null, Objects.requireNonNull(uniqueId), null, Duration.ZERO, null);
    }

    public static BanRequest myTeamSpeakId(String myTeamSpeakId) {
        return new BanRequest(null, null, null, Objects.requireNonNull(myTeamSpeakId), Duration.ZERO, null);
    }

    public BanRequest withIp(String ipPattern) {
        return new BanRequest(ipPattern, name, uniqueId, myTeamSpeakId, duration, reason);
    }

    public BanRequest withName(String namePattern) {
        return new BanRequest(ip, namePattern, uniqueId, myTeamSpeakId, duration, reason);
    }

    public BanRequest withUniqueId(String id) {
        return new BanRequest(ip, name, id, myTeamSpeakId, duration, reason);
    }

    public BanRequest withMyTeamSpeakId(String id) {
        return new BanRequest(ip, name, uniqueId, id, duration, reason);
    }

    /** @param banDuration the ban duration, truncated to seconds; {@link Duration#ZERO} is permanent */
    public BanRequest withDuration(Duration banDuration) {
        return new BanRequest(ip, name, uniqueId, myTeamSpeakId, banDuration, reason);
    }

    public BanRequest withReason(String banReason) {
        return new BanRequest(ip, name, uniqueId, myTeamSpeakId, duration, banReason);
    }
}
