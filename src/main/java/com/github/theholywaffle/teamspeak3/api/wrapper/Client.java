package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A connected client as listed by {@code clientlist}. The library requests every list option,
 * so the getters below require their properties; getters returning {@link Optional} cover values
 * that a client or server may leave unset.
 */
public class Client extends Wrapper {

    private static final Pattern BADGE_SEPARATOR = Pattern.compile("[\\s:;]");

    public Client(Map<String, String> map) {
        super(map);
    }

    public boolean canTalk() {
        return requireBoolean(ClientProperty.CLIENT_IS_TALKER);
    }

    /** @return the away message; empty if none is set */
    public String getAwayMessage() {
        return require(ClientProperty.CLIENT_AWAY_MESSAGE);
    }

    /** @return the GUIDs of the badges the client displays, in received order */
    public List<String> getBadgeGUIDs() {
        return badge("badges")
                .filter(badges -> !badges.isEmpty())
                .map(badges -> List.of(badges.split(",")))
                .orElse(List.of());
    }

    public int getChannelGroupId() {
        return requireInt(ClientProperty.CLIENT_CHANNEL_GROUP_ID);
    }

    public int getChannelId() {
        return requireInt(ClientProperty.CID);
    }

    /**
     * Utility method. This method will return a client URI.
     * A client URI can be used to reference a client in descriptions or just via chat.
     * Example: {@code client://<clientId>/<clientUId>~<clientNickname>}
     *
     * @return Client's URI
     */
    public String getClientURI() {
        return "client://" + getId().value() + '/' + getUniqueIdentifier() + '~'
                + URLEncoder.encode(getNickname(), StandardCharsets.UTF_8);
    }

    /** @return the two-letter country code, or empty if the server could not determine it */
    public Optional<String> getCountry() {
        return findText(ClientProperty.CLIENT_COUNTRY);
    }

    /** @return when the client identity was first seen, or empty if not recorded (such as for query clients) */
    public Optional<Instant> getCreatedDate() {
        return requireTimestamp(ClientProperty.CLIENT_CREATED);
    }

    public ClientDatabaseId getDatabaseId() {
        return requireDatabaseId(ClientProperty.CLIENT_DATABASE_ID);
    }

    /** @return the estimated location; TeamSpeak 3.13 does not send it, so this is usually empty */
    public Optional<String> getEstimatedLocation() {
        return findText(ClientProperty.CLIENT_ESTIMATED_LOCATION);
    }

    public long getIconId() {
        return requireLong(ClientProperty.CLIENT_ICON_ID);
    }

    public ClientId getId() {
        return requireClientId(ClientProperty.CLID);
    }

    /** @return the time since the client last sent voice or text */
    public Duration getIdleTime() {
        return requireMillis(ClientProperty.CLIENT_IDLE_TIME);
    }

    /** @return the ID of the channel from which the client's channel group is inherited */
    public int getInheritedChannelGroupId() {
        return requireInt(ClientProperty.CLIENT_CHANNEL_GROUP_INHERITED_CHANNEL_ID);
    }

    /** @return the client's IP address, or empty if the server does not reveal it */
    public Optional<String> getIp() {
        return findText(ClientProperty.CONNECTION_CLIENT_IP);
    }

    /** @return when the client last connected before this connection, or empty if not recorded */
    public Optional<Instant> getLastConnectedDate() {
        return requireTimestamp(ClientProperty.CLIENT_LASTCONNECTED);
    }

    public String getNickname() {
        return require(ClientProperty.CLIENT_NICKNAME);
    }

    public String getPlatform() {
        return require(ClientProperty.CLIENT_PLATFORM);
    }

    /** @return the IDs of the client's server groups, in received order */
    public List<Integer> getServerGroups() {
        return requireIntList(ClientProperty.CLIENT_SERVERGROUPS);
    }

    public int getTalkPower() {
        return requireInt(ClientProperty.CLIENT_TALK_POWER);
    }

    /** @return {@code 0} for a voice client, {@code 1} for a ServerQuery client; other values are retained */
    public int getType() {
        return requireInt(ClientProperty.CLIENT_TYPE);
    }

    public String getUniqueIdentifier() {
        return require(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
    }

    public String getVersion() {
        return require(ClientProperty.CLIENT_VERSION);
    }

    public boolean hasOverwolf() {
        return badge("overwolf").or(() -> badge("Overwolf")).filter("1"::equals).isPresent();
    }

    public boolean isAway() {
        return requireBoolean(ClientProperty.CLIENT_AWAY);
    }

    public boolean isChannelCommander() {
        return requireBoolean(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER);
    }

    /**
     * @param serverGroupId
     * 		the ID of the server group to search for
     *
     * @return whether {@link #getServerGroups()} contains the given server group ID
     */
    public boolean isInServerGroup(int serverGroupId) {
        return getServerGroups().contains(serverGroupId);
    }

    /**
     * @param serverGroup
     * 		the server group to search for
     *
     * @return whether {@link #getServerGroups()} contains the ID of the given server group
     */
    public boolean isInServerGroup(ServerGroup serverGroup) {
        return isInServerGroup(serverGroup.getId());
    }

    public boolean isInputHardware() {
        return requireBoolean(ClientProperty.CLIENT_INPUT_HARDWARE);
    }

    public boolean isInputMuted() {
        return requireBoolean(ClientProperty.CLIENT_INPUT_MUTED);
    }

    public boolean isOutputHardware() {
        return requireBoolean(ClientProperty.CLIENT_OUTPUT_HARDWARE);
    }

    public boolean isOutputMuted() {
        return requireBoolean(ClientProperty.CLIENT_OUTPUT_MUTED);
    }

    public boolean isPrioritySpeaker() {
        return requireBoolean(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER);
    }

    public boolean isRecording() {
        return requireBoolean(ClientProperty.CLIENT_IS_RECORDING);
    }

    public boolean isRegularClient() {
        return getType() == 0;
    }

    public boolean isServerQueryClient() {
        return getType() == 1;
    }

    public boolean isTalking() {
        return requireBoolean(ClientProperty.CLIENT_FLAG_TALKING);
    }

    /** Badge entries look like {@code overwolf=0:badges=guid1,guid2}. */
    private Optional<String> badge(String key) {
        String prefix = key + "=";
        for (String entry : BADGE_SEPARATOR.split(require(ClientProperty.CLIENT_BADGES), -1)) {
            if (entry.startsWith(prefix)) return Optional.of(entry.substring(prefix.length()));
        }
        return Optional.empty();
    }
}
