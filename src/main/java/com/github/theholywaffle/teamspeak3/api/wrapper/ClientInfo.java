package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Detailed information about a connected client from {@code clientinfo}, which does not echo the client ID. */
public class ClientInfo extends Client {

    private final ClientId clientId;

    public ClientInfo(ClientId clientId, Map<String, String> map) {
        super(map);
        this.clientId = Objects.requireNonNull(clientId, "clientId");
    }

    @Override
    public ClientId getId() {
        return clientId;
    }

    /** @return the avatar hash, or empty if the client has no avatar */
    public Optional<String> getAvatar() {
        return findText(ClientProperty.CLIENT_FLAG_AVATAR);
    }

    public long getBandwidthReceivedLastMinute() {
        return requireLong(ClientProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_MINUTE_TOTAL);
    }

    public long getBandwidthReceivedLastSecond() {
        return requireLong(ClientProperty.CONNECTION_BANDWIDTH_RECEIVED_LAST_SECOND_TOTAL);
    }

    public long getBandwidthSentlastMinute() {
        return requireLong(ClientProperty.CONNECTION_BANDWIDTH_SENT_LAST_MINUTE_TOTAL);
    }

    public long getBandwidthSentLastSecond() {
        return requireLong(ClientProperty.CONNECTION_BANDWIDTH_SENT_LAST_SECOND_TOTAL);
    }

    public String getBase64ClientUId() {
        return require(ClientProperty.CLIENT_BASE64HASHCLIENTUID);
    }

    /**
     * @return the ID of the channel the client asked to join on connect, or empty if none
     * @throws TS3InvalidValueException if the value is not a {@code /}-prefixed channel ID
     */
    public OptionalInt getDefaultChannel() {
        return findParsed(
                        ClientProperty.CLIENT_DEFAULT_CHANNEL.getName(),
                        "a /-prefixed channel ID",
                        ClientInfo::parseDefaultChannel)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    /** @return the privilege key the client used on connect, or empty if none */
    public Optional<String> getDefaultToken() {
        return findText(ClientProperty.CLIENT_DEFAULT_TOKEN);
    }

    public String getDescription() {
        return require(ClientProperty.CLIENT_DESCRIPTION);
    }

    public long getFiletransferBandwidthReceived() {
        return requireLong(ClientProperty.CONNECTION_FILETRANSFER_BANDWIDTH_RECEIVED);
    }

    public long getFiletransferBandwidthSent() {
        return requireLong(ClientProperty.CONNECTION_FILETRANSFER_BANDWIDTH_SENT);
    }

    /** @return the ServerQuery login name, or empty if the client is not logged in to a query account */
    public Optional<String> getLoginName() {
        return findText(ClientProperty.CLIENT_LOGIN_NAME);
    }

    public String getMetaData() {
        return require(ClientProperty.CLIENT_META_DATA);
    }

    public long getMonthlyBytesDownloaded() {
        return requireLong(ClientProperty.CLIENT_MONTH_BYTES_DOWNLOADED);
    }

    public long getMonthlyBytesUploaded() {
        return requireLong(ClientProperty.CLIENT_MONTH_BYTES_UPLOADED);
    }

    /** @return the myTeamSpeak ID, or empty if the client has none */
    public Optional<String> getMyTeamSpeakId() {
        return findText(ClientProperty.CLIENT_MYTEAMSPEAK_ID);
    }

    public int getNeededServerQueryViewPower() {
        return requireInt(ClientProperty.CLIENT_NEEDED_SERVERQUERY_VIEW_POWER);
    }

    public String getPhoneticNickname() {
        return require(ClientProperty.CLIENT_NICKNAME_PHONETIC);
    }

    /** @return when the pending talk request was made, or empty if the client is not requesting to talk */
    public Optional<Instant> getTalkRequestCreatedDate() {
        return requireTimestamp(ClientProperty.CLIENT_TALK_REQUEST);
    }

    public String getTalkRequestMessage() {
        return require(ClientProperty.CLIENT_TALK_REQUEST_MSG);
    }

    public Duration getTimeConnected() {
        return requireMillis(ClientProperty.CONNECTION_CONNECTED_TIME);
    }

    public long getTotalBytesDownloaded() {
        return requireLong(ClientProperty.CLIENT_TOTAL_BYTES_DOWNLOADED);
    }

    public long getTotalBytesReceived() {
        return requireLong(ClientProperty.CONNECTION_BYTES_RECEIVED_TOTAL);
    }

    public long getTotalBytesSent() {
        return requireLong(ClientProperty.CONNECTION_BYTES_SENT_TOTAL);
    }

    public long getTotalBytesUploaded() {
        return requireLong(ClientProperty.CLIENT_TOTAL_BYTES_UPLOADED);
    }

    public int getTotalConnections() {
        return requireInt(ClientProperty.CLIENT_TOTALCONNECTIONS);
    }

    public long getTotalPacketsReceived() {
        return requireLong(ClientProperty.CONNECTION_PACKETS_RECEIVED_TOTAL);
    }

    public long getTotalPacketsSent() {
        return requireLong(ClientProperty.CONNECTION_PACKETS_SENT_TOTAL);
    }

    /** @return the number of unread offline messages; TeamSpeak only reports it for the query's own client */
    public OptionalInt getUnreadMessages() {
        return findInt(ClientProperty.CLIENT_UNREAD_MESSAGES);
    }

    public boolean isOutputOnlyMuted() {
        return requireBoolean(ClientProperty.CLIENT_OUTPUTONLY_MUTED);
    }

    public boolean isRequestingToTalk() {
        return getTalkRequestCreatedDate().isPresent();
    }

    @Override
    public boolean isTalking() {
        throw new TS3UnsupportedOperationException(
                "clientinfo does not report talk status; use Client#isTalking from clientlist");
    }

    private static int parseDefaultChannel(String value) {
        if (!value.startsWith("/")) throw new IllegalArgumentException("Missing / prefix");
        return Integer.parseInt(value.substring(1));
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other) && clientId.equals(((ClientInfo) other).clientId);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + clientId.hashCode();
    }
}
