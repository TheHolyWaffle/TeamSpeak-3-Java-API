package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prints every connected client with the name of its channel, then disconnects.
 */
public final class ClientInfoExample {

    private ClientInfoExample() {}

    public static void main(String[] args) {
        ExampleSettings settings = ExampleSettings.fromEnvironment();
        try (TS3Query query = new TS3Query(settings.config())) {
            // The configured login is sent while connecting.
            query.connect();

            TS3Api api = query.getApi();
            api.selectVirtualServerById(settings.virtualServerId());
            api.setNickname(settings.nickname());

            Map<Integer, Channel> channels =
                    api.channels().list().stream().collect(Collectors.toMap(Channel::getId, Function.identity()));

            System.out.println("Server version: " + api.instance().version().version());
            for (Client client : api.clients().list()) {
                Channel channel = channels.get(client.getChannelId());
                String channelName = channel == null ? "an unknown channel" : channel.getName();
                System.out.println(client.getNickname() + " in channel " + channelName);
            }

            // Drains outstanding commands and sends quit before the query is closed.
            query.exit();
        }
    }
}
