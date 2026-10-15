package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3BaseApi;
import com.github.theholywaffle.teamspeak3.TS3WebQuery;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.exception.TS3UnsupportedOperationException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import java.util.Map;

/**
 * Administers a virtual server over HTTP(S) Web Query with an API key: no login, server selection or
 * connection. Creates a channel, reads it back and deletes it again, so it can be run repeatedly.
 */
public final class WebQueryExample {

    private WebQueryExample() {}

    public static void main(String[] args) {
        ExampleSettings settings = ExampleSettings.fromEnvironment();
        try (TS3WebQuery query = new TS3WebQuery(settings.webQueryConfig())) {
            System.out.println(
                    "Server version: " + query.getApi().instance().version().version());

            // Every command of this API addresses the virtual server in its request URL.
            TS3BaseApi api = query.getApi(settings.virtualServerId());
            for (Client client : api.clients().list()) {
                System.out.println(client.getNickname() + " is online");
            }

            ChannelId channelId = api.channels()
                    .create(
                            "Java API Web Query example",
                            Map.of(
                                    ChannelProperty.CHANNEL_FLAG_PERMANENT, "1",
                                    ChannelProperty.CHANNEL_TOPIC, "Created over Web Query"));
            try {
                System.out.println("Created channel " + channelId + " with topic \""
                        + api.channels().info(channelId).getTopic() + "\"");
            } finally {
                api.channels().delete(channelId, true);
                System.out.println("Deleted channel " + channelId);
            }

            try {
                api.events().registerAll();
            } catch (TS3UnsupportedOperationException e) {
                System.out.println("Events need a RAW or SSH query: " + e.getMessage());
            }
        }
    }
}
