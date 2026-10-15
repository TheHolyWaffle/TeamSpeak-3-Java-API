package com.github.theholywaffle.teamspeak3.example;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Pokes, messages, kicks and moves clients who join the server at random intervals, using the
 * asynchronous API from a single scheduler thread. Runs until Enter or end of input.
 */
public final class ScheduledActionsExample {

    private static final String[] MESSAGES = {
        "Hello from the Java API example.",
        "This is a scheduled test message.",
        "You are connected to a test server.",
        "Scheduled actions run every 30 to 60 seconds."
    };

    private ScheduledActionsExample() {}

    public static void main(String[] args) {
        ExampleSettings settings = ExampleSettings.fromEnvironment();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Map<ClientId, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
        try (TS3Query query = new TS3Query(settings.config())) {
            query.connect();

            TS3Api api = query.getApi();
            api.selectVirtualServerById(settings.virtualServerId());
            api.setNickname(settings.nickname());

            // The asynchronous API never blocks the single scheduler thread.
            TS3ApiAsync async = query.getAsyncApi();
            api.events().subscribe(new TS3EventAdapter() {
                @Override
                public void onClientJoin(ClientJoinEvent e) {
                    ClientId client = e.getClientId();
                    tasks.put(
                            client,
                            scheduler.scheduleWithFixedDelay(
                                    () -> act(async, client), random(10, 20), random(30, 60), TimeUnit.SECONDS));
                }

                @Override
                public void onClientLeave(ClientLeaveEvent e) {
                    ScheduledFuture<?> task = tasks.remove(e.getClientId());
                    if (task != null) task.cancel(false);
                }
            });
            api.events().register(TS3EventType.SERVER);
            System.out.println("Waiting for clients to join.");

            ExampleSettings.awaitStop();
            query.exit();
        } finally {
            scheduler.shutdownNow();
        }
    }

    private static void act(TS3ApiAsync api, ClientId client) {
        String message = MESSAGES[ThreadLocalRandom.current().nextInt(MESSAGES.length)];
        CompletableFuture<Void> action = switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> api.clients().poke(client, message);
            case 1 -> api.messages().sendPrivate(client, message);
            case 2 -> api.clients().kickFromChannel(message, client);
            default ->
                api.channels().list().thenCompose(channels -> {
                    Channel target = channels.get(ThreadLocalRandom.current().nextInt(channels.size()));
                    return api.clients().move(client, target.getId());
                });
        };
        action.exceptionally(failure -> {
            System.err.println("Could not act on client " + client + ": " + failure.getMessage());
            return null;
        });
    }

    private static long random(int min, int max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }
}
