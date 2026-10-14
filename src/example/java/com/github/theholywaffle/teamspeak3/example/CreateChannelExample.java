package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import java.util.Map;

/**
 * Creates a permanent sub-channel of the query's current channel, reads it back and deletes it again,
 * so the example can be run repeatedly.
 */
public final class CreateChannelExample {

    private CreateChannelExample() {}

    public static void main(String[] args) {
        ExampleSettings settings = ExampleSettings.fromEnvironment();
        try (TS3Query query = new TS3Query(settings.config())) {
            query.connect();

            TS3Api api = query.getApi();
            api.selectVirtualServerById(settings.virtualServerId());
            api.setNickname(settings.nickname());

            Map<ChannelProperty, String> properties = Map.of(
                    ChannelProperty.CHANNEL_FLAG_PERMANENT, "1",
                    ChannelProperty.CPID, String.valueOf(api.whoAmI().getChannelId()),
                    ChannelProperty.CHANNEL_TOPIC, "Created by the Java API example");

            int channelId = api.channels().create("Java API example channel", properties);
            try {
                ChannelInfo info = api.channels().info(channelId);
                System.out.println("Created channel " + channelId + " \"" + info.getName() + "\" with topic \""
                        + info.getTopic() + "\"");
            } finally {
                api.channels().delete(channelId, true);
                System.out.println("Deleted channel " + channelId);
            }

            query.exit();
        }
    }
}
