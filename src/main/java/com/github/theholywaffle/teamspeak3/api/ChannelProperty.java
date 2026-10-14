package com.github.theholywaffle.teamspeak3.api;

import java.util.Locale;

public enum ChannelProperty implements Property {
    CHANNEL_BANNER_GFX_URL(true),
    CHANNEL_BANNER_MODE(true),
    CHANNEL_CODEC(true),
    CHANNEL_CODEC_IS_UNENCRYPTED(true),
    CHANNEL_CODEC_LATENCY_FACTOR(false),
    CHANNEL_CODEC_QUALITY(true),
    CHANNEL_DELETE_DELAY(true),
    CHANNEL_DESCRIPTION(true),
    CHANNEL_FILEPATH(false),
    CHANNEL_FLAG_DEFAULT(true),
    CHANNEL_FLAG_MAXCLIENTS_UNLIMITED(true),
    CHANNEL_FLAG_MAXFAMILYCLIENTS_INHERITED(true),
    CHANNEL_FLAG_MAXFAMILYCLIENTS_UNLIMITED(true),
    CHANNEL_FLAG_PASSWORD(false),
    CHANNEL_FLAG_PERMANENT(true),
    CHANNEL_FLAG_SEMI_PERMANENT(true),
    CHANNEL_FLAG_TEMPORARY(true),
    CHANNEL_FORCED_SILENCE(false),
    /**
     * The unsigned icon ID shown for this channel. TS3 3.13 rejects this property in edits; assign an icon
     * through the {@code i_icon_id} permission instead, for example
     * {@code permissions().add(PermissionTarget.channel(channelId), "i_icon_id", (int) iconId)}.
     */
    CHANNEL_ICON_ID(true),
    CHANNEL_MAXCLIENTS(true),
    CHANNEL_MAXFAMILYCLIENTS(true),
    CHANNEL_NAME(true),
    CHANNEL_NAME_PHONETIC(true),
    CHANNEL_NEEDED_SUBSCRIBE_POWER(false),
    CHANNEL_NEEDED_TALK_POWER(true),
    CHANNEL_ORDER(true),
    CHANNEL_PASSWORD(true),
    CHANNEL_SECURITY_SALT(false),
    CHANNEL_TOPIC(true),
    CHANNEL_UNIQUE_IDENTIFIER(false),
    CID(false),
    CPID(true),
    PID(false),
    SECONDS_EMPTY(false),
    TOTAL_CLIENTS(false),
    TOTAL_CLIENTS_FAMILY(false);

    private final boolean changeable;

    ChannelProperty(boolean changeable) {
        this.changeable = changeable;
    }

    @Override
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean isChangeable() {
        return changeable;
    }
}
