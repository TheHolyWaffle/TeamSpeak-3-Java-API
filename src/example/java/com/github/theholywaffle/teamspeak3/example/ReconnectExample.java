package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.reconnect.CommandRetryPolicy;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.reconnect.SessionConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Reconnects automatically after a connection loss. Restart the server while the example runs
 * to watch it reconnect.
 * Runs until Enter or end of input.
 */
public final class ReconnectExample {

    // The query's client ID changes on every connection; event callbacks read it from other threads.
    private static volatile ClientId clientId;

    private ReconnectExample() {}

    public static void main(String[] args) {
        ExampleSettings settings = ExampleSettings.fromEnvironment();
        TS3Config config = settings.config()
                // Restored, in this order, after the configured login on every connection and before any
                // queued command is sent.
                .setSessionConfiguration(SessionConfiguration.forServer(settings.virtualServerId())
                        .withNickname(settings.nickname())
                        .withSubscription(TS3EventType.TEXT_CHANNEL))
                // Ten attempts with jittered exponential backoff, then the query closes.
                .setReconnectStrategy(ReconnectStrategy.exponentialBackoff())
                // Only explicitly named reads are replayed; a sent channelcreate is never sent twice.
                .setCommandRetryPolicy(CommandRetryPolicy.safeReads(1, "whoami"))
                .setConnectionHandler(new ConnectionHandler() {
                    @Override
                    public void onConnect(TS3Api api) {
                        clientId = api.whoAmI().getId().orElseThrow();
                        System.out.println("Connected as client " + clientId);
                    }

                    @Override
                    public void onDisconnect(TS3Query query) {
                        System.out.println("Disconnected; reconnecting");
                    }
                });

        try (TS3Query query = new TS3Query(config)) {
            query.connect();

            // Listeners belong to the query and survive reconnects; register them only once.
            TS3Api api = query.getApi();
            api.events().subscribe(new TS3Listener() {
                @Override
                public void onTextMessage(TextMessageEvent e) {
                    if (e.getTargetMode() == TextMessageTargetMode.CHANNEL
                            && !e.getInvokerId().equals(Optional.ofNullable(clientId))) {
                        api.messages().sendChannel("Hello " + e.getInvokerName().orElse("there") + "!");
                    }
                }
            });

            createAndDeleteChannels(query.getAsyncApi());

            ExampleSettings.awaitStop();
            query.exit();
        }
    }

    private static void createAndDeleteChannels(TS3ApiAsync api) {
        List<CompletableFuture<ChannelId>> creates = new ArrayList<>();
        for (char letter : "Hello".toCharArray()) {
            String name = "Reconnect example " + letter;
            creates.add(api.channels().create(name, Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
        }

        // Unsent commands survive a reconnect in order. A sent mutation whose response was lost
        // fails instead: reconcile with the server before deciding to retry it.
        List<ChannelId> created = new ArrayList<>();
        for (CompletableFuture<ChannelId> create : creates) {
            try {
                created.add(create.join());
            } catch (CompletionException e) {
                System.err.println("Channel creation failed: " + e.getCause().getMessage());
            }
        }
        System.out.println("Created channels " + created);

        for (ChannelId channelId : created) {
            api.channels().delete(channelId, true).join();
        }
        System.out.println("Deleted channels " + created);
    }
}
