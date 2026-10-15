package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.BanId;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * A ban rule from {@code banlist}. A rule matches on any combination of IP pattern, name pattern,
 * unique ID and myTeamSpeak ID; criteria that are not part of the rule are empty.
 */
public class Ban extends Wrapper {

    public Ban(Map<String, String> map) {
        super(map);
    }

    public BanId getId() {
        return requireId("banid", BanId::of);
    }

    /** @return the banned IP pattern, or empty if the rule does not match on IP */
    public Optional<String> getBannedIp() {
        return findText("ip");
    }

    /** @return the banned name pattern, or empty if the rule does not match on name */
    public Optional<String> getBannedName() {
        return findText("name");
    }

    /** @return the banned unique ID, or empty if the rule does not match on unique ID */
    public Optional<String> getBannedUId() {
        return findText("uid");
    }

    /** @return the banned myTeamSpeak ID, or empty if the rule does not match on it */
    public Optional<String> getBannedMyTeamSpeakId() {
        return findText("mytsid");
    }

    /** @return the last nickname of the banned client, or empty if unknown */
    public Optional<String> getLastNickname() {
        return findText("lastnickname");
    }

    public Instant getCreatedDate() {
        return requireInstant("created");
    }

    /** @return the ban duration with second precision; {@link Duration#ZERO} for a permanent ban */
    public Duration getDuration() {
        return requireSeconds("duration");
    }

    public boolean isPermanent() {
        return getDuration().isZero();
    }

    public String getInvokerName() {
        return require("invokername");
    }

    /** @return the database ID of the client who created the ban, or empty if none ({@code 0}) */
    public Optional<ClientDatabaseId> getInvokerClientDBId() {
        return findDatabaseId("invokercldbid");
    }

    public String getInvokerUId() {
        return require("invokeruid");
    }

    /** @return the reason, or empty if none was given */
    public Optional<String> getReason() {
        return findText("reason");
    }

    public int getEnforcements() {
        return requireInt("enforcements");
    }
}
