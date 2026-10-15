package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import java.util.Locale;
import java.util.Optional;

/**
 * A chat bot in the query's current channel: it answers "!ping" with "pong" and greets every
 * client whose message starts with "hello". Runs until Enter or end of input.
 */
public final class ChatBotExample {

    private ChatBotExample() {}

    public static void main(String[] args) {
        ExampleSettings settings = ExampleSettings.fromEnvironment();
        try (TS3Query query = new TS3Query(settings.config())) {
            query.connect();

            TS3Api api = query.getApi();
            api.selectVirtualServerById(settings.virtualServerId());
            api.setNickname(settings.nickname());
            ClientId self = api.whoAmI().getId().orElseThrow();

            // Listeners run on the query's event threads; the subscription is active until the query closes.
            api.events().subscribe(new TS3EventAdapter() {
                @Override
                public void onTextMessage(TextMessageEvent e) {
                    if (e.getTargetMode() != TextMessageTargetMode.CHANNEL
                            || e.getInvokerId().equals(Optional.of(self))) {
                        return;
                    }
                    String message = e.getMessage().toLowerCase(Locale.ROOT);
                    if (message.equals("!ping")) {
                        api.messages().sendChannel("pong");
                    } else if (message.startsWith("hello")) {
                        api.messages().sendChannel("Hello " + e.getInvokerName().orElse("there") + "!");
                    }
                }
            });
            // Asks the server to send messages written in the query's current channel.
            api.events().register(TS3EventType.TEXT_CHANNEL);
            api.messages().sendChannel(settings.nickname() + " is online. Say !ping or hello.");

            ExampleSettings.awaitStop();
            query.exit();
        }
    }
}
