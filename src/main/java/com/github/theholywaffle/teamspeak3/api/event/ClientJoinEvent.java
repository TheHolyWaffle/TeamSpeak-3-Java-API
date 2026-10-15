package com.github.theholywaffle.teamspeak3.api.event;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupId;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.IconId;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ClientJoinEvent extends BaseEvent {

    private final AtomicReference<List<ServerGroupId>> serverGroups = new AtomicReference<>();

    public ClientJoinEvent(Wrapper wrapper) {
        super(wrapper);
    }

    /** @return the channel the client came from, or empty when it connected to the server ({@code 0}) */
    public Optional<ChannelId> getClientFromId() {
        return requireOptionalId("cfid", ChannelId::of);
    }

    /** @return the channel the client joined */
    public ChannelId getClientTargetId() {
        return requireId("ctid", ChannelId::of);
    }

    public ClientId getClientId() {
        return requireClientId("clid");
    }

    public String getUniqueClientIdentifier() {
        return require(ClientProperty.CLIENT_UNIQUE_IDENTIFIER);
    }

    public String getClientNickname() {
        return require(ClientProperty.CLIENT_NICKNAME);
    }

    public boolean isClientInputMuted() {
        return requireBoolean(ClientProperty.CLIENT_INPUT_MUTED);
    }

    public boolean isClientOutputMuted() {
        return requireBoolean(ClientProperty.CLIENT_OUTPUT_MUTED);
    }

    public boolean isClientOutputOnlyMuted() {
        return requireBoolean(ClientProperty.CLIENT_OUTPUTONLY_MUTED);
    }

    public boolean isClientUsingHardwareInput() {
        return requireBoolean(ClientProperty.CLIENT_INPUT_HARDWARE);
    }

    public boolean isClientUsingHardwareOutput() {
        return requireBoolean(ClientProperty.CLIENT_OUTPUT_HARDWARE);
    }

    public String getClientMetadata() {
        return require(ClientProperty.CLIENT_META_DATA);
    }

    public boolean isClientRecording() {
        return requireBoolean(ClientProperty.CLIENT_IS_RECORDING);
    }

    public ClientDatabaseId getClientDatabaseId() {
        return requireDatabaseId(ClientProperty.CLIENT_DATABASE_ID);
    }

    public ChannelGroupId getClientChannelGroupId() {
        return requireId(ClientProperty.CLIENT_CHANNEL_GROUP_ID, ChannelGroupId::of);
    }

    public int getAmountOfServerGroups() {
        return getClientServerGroups().size();
    }

    /** @return the IDs of the client's server groups, in received order */
    public List<ServerGroupId> getClientServerGroups() {
        return requireServerGroups(serverGroups);
    }

    public boolean isClientAway() {
        return requireBoolean(ClientProperty.CLIENT_AWAY);
    }

    public String getClientAwayMessage() {
        return require(ClientProperty.CLIENT_AWAY_MESSAGE);
    }

    public int getClientType() {
        return requireInt(ClientProperty.CLIENT_TYPE);
    }

    /** @return the avatar hash, or empty if the client has no avatar */
    public Optional<String> getClientFlagAvatarId() {
        return findText(ClientProperty.CLIENT_FLAG_AVATAR);
    }

    public int getClientTalkPower() {
        return requireInt(ClientProperty.CLIENT_TALK_POWER);
    }

    /** {@code client_talk_request} holds the request time in epoch seconds, or {@code 0} without a request. */
    public boolean isClientRequestingToTalk() {
        return requireTimestamp(ClientProperty.CLIENT_TALK_REQUEST).isPresent();
    }

    public String getClientTalkRequestMessage() {
        return require(ClientProperty.CLIENT_TALK_REQUEST_MSG);
    }

    public String getClientDescription() {
        return require(ClientProperty.CLIENT_DESCRIPTION);
    }

    public boolean isClientTalking() {
        return requireBoolean(ClientProperty.CLIENT_IS_TALKER);
    }

    public boolean isClientPrioritySpeaker() {
        return requireBoolean(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER);
    }

    public int getClientUnreadMessages() {
        return requireInt(ClientProperty.CLIENT_UNREAD_MESSAGES);
    }

    public String getClientPhoneticNickname() {
        return require(ClientProperty.CLIENT_NICKNAME_PHONETIC);
    }

    public int getClientNeededServerQueryViewPower() {
        return requireInt(ClientProperty.CLIENT_NEEDED_SERVERQUERY_VIEW_POWER);
    }

    /** @return the client's icon, or empty if it has none */
    public Optional<IconId> getClientIconId() {
        return requireIconId(ClientProperty.CLIENT_ICON_ID);
    }

    public boolean isClientChannelCommander() {
        return requireBoolean(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER);
    }

    /** @return the two-letter country code, or empty if the server could not determine it */
    public Optional<String> getClientCountry() {
        return findText(ClientProperty.CLIENT_COUNTRY);
    }

    /** @return the ID of the channel from which the client's channel group is inherited */
    public ChannelId getClientInheritedChannelGroupId() {
        return requireId(ClientProperty.CLIENT_CHANNEL_GROUP_INHERITED_CHANNEL_ID, ChannelId::of);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onClientJoin(this);
    }
}
