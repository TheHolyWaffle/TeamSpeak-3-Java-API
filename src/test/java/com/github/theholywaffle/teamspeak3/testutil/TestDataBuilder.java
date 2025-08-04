package com.github.theholywaffle.teamspeak3.testutil;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for creating test data objects with realistic default values.
 * This class provides a fluent API for building test objects with customizable properties.
 */
public class TestDataBuilder {

    /**
     * Builder for creating Client test objects.
     */
    public static class ClientBuilder {
        private final Map<String, String> properties = new HashMap<>();

        public ClientBuilder() {
            // Set realistic default values
            properties.put(ClientProperty.CLID.getName(), "1");
            properties.put(ClientProperty.CID.getName(), "1");
            properties.put(ClientProperty.CLIENT_NICKNAME.getName(), "TestClient");
            properties.put(ClientProperty.CLIENT_UNIQUE_IDENTIFIER.getName(), "unique123456789");
            properties.put(ClientProperty.CLIENT_DATABASE_ID.getName(), "100");
            properties.put(ClientProperty.CLIENT_CHANNEL_GROUP_ID.getName(), "8");
            properties.put(ClientProperty.CLIENT_SERVERGROUPS.getName(), "6,7,8");
            properties.put(ClientProperty.CLIENT_TYPE.getName(), "0");
            properties.put(ClientProperty.CLIENT_AWAY.getName(), "0");
            properties.put(ClientProperty.CLIENT_AWAY_MESSAGE.getName(), "");
            properties.put(ClientProperty.CLIENT_IS_TALKER.getName(), "1");
            properties.put(ClientProperty.CLIENT_IS_CHANNEL_COMMANDER.getName(), "0");
            properties.put(ClientProperty.CLIENT_IS_PRIORITY_SPEAKER.getName(), "0");
            properties.put(ClientProperty.CLIENT_IS_RECORDING.getName(), "0");
            properties.put(ClientProperty.CLIENT_FLAG_TALKING.getName(), "0");
            properties.put(ClientProperty.CLIENT_INPUT_MUTED.getName(), "0");
            properties.put(ClientProperty.CLIENT_OUTPUT_MUTED.getName(), "0");
            properties.put(ClientProperty.CLIENT_INPUT_HARDWARE.getName(), "1");
            properties.put(ClientProperty.CLIENT_OUTPUT_HARDWARE.getName(), "1");
            properties.put(ClientProperty.CLIENT_TALK_POWER.getName(), "75");
            properties.put(ClientProperty.CLIENT_CREATED.getName(), "1609459200");
            properties.put(ClientProperty.CLIENT_LASTCONNECTED.getName(), "1609545600");
            properties.put(ClientProperty.CLIENT_IDLE_TIME.getName(), "0");
            properties.put(ClientProperty.CLIENT_VERSION.getName(), "3.5.6");
            properties.put(ClientProperty.CLIENT_PLATFORM.getName(), "Windows");
            properties.put(ClientProperty.CLIENT_COUNTRY.getName(), "US");
            properties.put(ClientProperty.CLIENT_ESTIMATED_LOCATION.getName(), "New York");
            properties.put(ClientProperty.CONNECTION_CLIENT_IP.getName(), "192.168.1.100");
            properties.put(ClientProperty.CLIENT_ICON_ID.getName(), "0");
            properties.put(ClientProperty.CLIENT_BADGES.getName(), "overwolf=0");
        }

        public ClientBuilder withId(int id) {
            properties.put(ClientProperty.CLID.getName(), String.valueOf(id));
            return this;
        }

        public ClientBuilder withNickname(String nickname) {
            properties.put(ClientProperty.CLIENT_NICKNAME.getName(), nickname);
            return this;
        }

        public ClientBuilder withChannelId(int channelId) {
            properties.put(ClientProperty.CID.getName(), String.valueOf(channelId));
            return this;
        }

        public ClientBuilder withUniqueIdentifier(String uniqueId) {
            properties.put(ClientProperty.CLIENT_UNIQUE_IDENTIFIER.getName(), uniqueId);
            return this;
        }

        public ClientBuilder withDatabaseId(int databaseId) {
            properties.put(ClientProperty.CLIENT_DATABASE_ID.getName(), String.valueOf(databaseId));
            return this;
        }

        public ClientBuilder withServerGroups(int... serverGroups) {
            if (serverGroups.length == 0) {
                properties.put(ClientProperty.CLIENT_SERVERGROUPS.getName(), "");
                return this;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < serverGroups.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(serverGroups[i]);
            }
            properties.put(ClientProperty.CLIENT_SERVERGROUPS.getName(), sb.toString());
            return this;
        }

        public ClientBuilder withTalkPower(int talkPower) {
            properties.put(ClientProperty.CLIENT_TALK_POWER.getName(), String.valueOf(talkPower));
            return this;
        }

        public ClientBuilder withAway(boolean away, String message) {
            properties.put(ClientProperty.CLIENT_AWAY.getName(), away ? "1" : "0");
            properties.put(ClientProperty.CLIENT_AWAY_MESSAGE.getName(), message != null ? message : "");
            return this;
        }

        public ClientBuilder withTalking(boolean talking) {
            properties.put(ClientProperty.CLIENT_FLAG_TALKING.getName(), talking ? "1" : "0");
            return this;
        }

        public ClientBuilder withMuted(boolean inputMuted, boolean outputMuted) {
            properties.put(ClientProperty.CLIENT_INPUT_MUTED.getName(), inputMuted ? "1" : "0");
            properties.put(ClientProperty.CLIENT_OUTPUT_MUTED.getName(), outputMuted ? "1" : "0");
            return this;
        }

        public ClientBuilder withBadges(String... badges) {
            StringBuilder sb = new StringBuilder();
            if (badges.length > 0) {
                sb.append("badges=");
                for (int i = 0; i < badges.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(badges[i]);
                }
                sb.append(" ");
            }
            sb.append("overwolf=0");
            properties.put(ClientProperty.CLIENT_BADGES.getName(), sb.toString());
            return this;
        }

        public ClientBuilder withOverwolf(boolean hasOverwolf) {
            String current = properties.get(ClientProperty.CLIENT_BADGES.getName());
            if (current.contains("overwolf=")) {
                current = current.replaceAll("overwolf=[01]", "overwolf=" + (hasOverwolf ? "1" : "0"));
            } else {
                current += " overwolf=" + (hasOverwolf ? "1" : "0");
            }
            properties.put(ClientProperty.CLIENT_BADGES.getName(), current);
            return this;
        }

        public Client build() {
            return new Client(new HashMap<>(properties));
        }
    }

    /**
     * Builder for creating Channel test objects.
     */
    public static class ChannelBuilder {
        private final Map<String, String> properties = new HashMap<>();

        public ChannelBuilder() {
            // Set realistic default values
            properties.put(ChannelProperty.CID.getName(), "1");
            properties.put(ChannelProperty.PID.getName(), "0");
            properties.put(ChannelProperty.CHANNEL_ORDER.getName(), "0");
            properties.put(ChannelProperty.CHANNEL_NAME.getName(), "Test Channel");
            properties.put(ChannelProperty.CHANNEL_TOPIC.getName(), "Test Topic");
            properties.put(ChannelProperty.CHANNEL_FLAG_DEFAULT.getName(), "0");
            properties.put(ChannelProperty.CHANNEL_FLAG_PASSWORD.getName(), "0");
            properties.put(ChannelProperty.CHANNEL_FLAG_PERMANENT.getName(), "1");
            properties.put(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT.getName(), "0");
            properties.put(ChannelProperty.CHANNEL_CODEC.getName(), "4");
            properties.put(ChannelProperty.CHANNEL_CODEC_QUALITY.getName(), "7");
            properties.put(ChannelProperty.CHANNEL_MAXCLIENTS.getName(), "-1");
            properties.put(ChannelProperty.CHANNEL_MAXFAMILYCLIENTS.getName(), "-1");
            properties.put(ChannelProperty.CHANNEL_NEEDED_TALK_POWER.getName(), "0");
            properties.put(ChannelProperty.CHANNEL_NEEDED_SUBSCRIBE_POWER.getName(), "0");
            properties.put(ChannelProperty.TOTAL_CLIENTS.getName(), "0");
            properties.put(ChannelProperty.TOTAL_CLIENTS_FAMILY.getName(), "0");
            properties.put(ChannelProperty.CHANNEL_ICON_ID.getName(), "0");
            properties.put(ChannelProperty.SECONDS_EMPTY.getName(), "0");
        }

        public ChannelBuilder withId(int id) {
            properties.put(ChannelProperty.CID.getName(), String.valueOf(id));
            return this;
        }

        public ChannelBuilder withName(String name) {
            properties.put(ChannelProperty.CHANNEL_NAME.getName(), name);
            return this;
        }

        public ChannelBuilder withParentId(int parentId) {
            properties.put(ChannelProperty.PID.getName(), String.valueOf(parentId));
            return this;
        }

        public ChannelBuilder withTopic(String topic) {
            properties.put(ChannelProperty.CHANNEL_TOPIC.getName(), topic);
            return this;
        }

        public ChannelBuilder withPassword(boolean hasPassword) {
            properties.put(ChannelProperty.CHANNEL_FLAG_PASSWORD.getName(), hasPassword ? "1" : "0");
            return this;
        }

        public ChannelBuilder withPermanent(boolean permanent) {
            properties.put(ChannelProperty.CHANNEL_FLAG_PERMANENT.getName(), permanent ? "1" : "0");
            return this;
        }

        public ChannelBuilder withMaxClients(int maxClients) {
            properties.put(ChannelProperty.CHANNEL_MAXCLIENTS.getName(), String.valueOf(maxClients));
            return this;
        }

        public ChannelBuilder withClientCounts(int totalClients, int totalClientsFamily) {
            properties.put(ChannelProperty.TOTAL_CLIENTS.getName(), String.valueOf(totalClients));
            properties.put(ChannelProperty.TOTAL_CLIENTS_FAMILY.getName(), String.valueOf(totalClientsFamily));
            return this;
        }

        public ChannelBuilder withTalkPower(int neededTalkPower) {
            properties.put(ChannelProperty.CHANNEL_NEEDED_TALK_POWER.getName(), String.valueOf(neededTalkPower));
            return this;
        }

        public ChannelBuilder withIconId(long iconId) {
            properties.put(ChannelProperty.CHANNEL_ICON_ID.getName(), String.valueOf(iconId));
            return this;
        }

        public Channel build() {
            return new Channel(new HashMap<>(properties));
        }
    }

    /**
     * Create a new ClientBuilder with default values.
     * @return a new ClientBuilder instance
     */
    public static ClientBuilder client() {
        return new ClientBuilder();
    }

    /**
     * Create a new ChannelBuilder with default values.
     * @return a new ChannelBuilder instance
     */
    public static ChannelBuilder channel() {
        return new ChannelBuilder();
    }
}
