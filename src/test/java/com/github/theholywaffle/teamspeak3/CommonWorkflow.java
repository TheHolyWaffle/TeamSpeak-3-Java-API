package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.PermissionTarget;
import com.github.theholywaffle.teamspeak3.api.UploadMode;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** The common workflow measured in docs/api-migration.md; keep both forms identical to the guide. */
final class CommonWorkflow {
    private CommonWorkflow() {}

    // workflow-sync-start
    static int prepareLobby(TS3Api api, String uniqueId, byte[] rules) {
        int lobby = api.channels()
                .findByName("Lobby", false)
                .map(Channel::getId)
                .orElseGet(() -> api.channels().create("Lobby", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1")));
        api.permissions().add(PermissionTarget.channel(lobby), "i_channel_needed_join_power", 10);
        api.files().upload(FileLocation.of(lobby, "/rules.txt"), rules, UploadMode.OVERWRITE);
        api.clients()
                .findByUniqueId(uniqueId)
                .ifPresentOrElse(
                        client -> {
                            api.clients().move(client.getId(), lobby);
                            api.messages().sendPrivate(client.getId(), "Welcome to the lobby");
                        },
                        () -> api.messages().sendOffline(uniqueId, "Lobby", "Welcome to the lobby"));
        return lobby;
    }
    // workflow-sync-end

    // workflow-async-start
    static CompletableFuture<Integer> prepareLobby(TS3ApiAsync api, String uniqueId, byte[] rules) {
        return api.channels()
                .findByName("Lobby", false)
                .thenCompose(found -> found.map(channel -> CompletableFuture.completedFuture(channel.getId()))
                        .orElseGet(() ->
                                api.channels().create("Lobby", Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"))))
                .thenCompose(lobby -> api.permissions()
                        .add(PermissionTarget.channel(lobby), "i_channel_needed_join_power", 10)
                        .thenCompose(done ->
                                api.files().upload(FileLocation.of(lobby, "/rules.txt"), rules, UploadMode.OVERWRITE))
                        .thenCompose(done -> api.clients().findByUniqueId(uniqueId))
                        .thenCompose(client -> client.isEmpty()
                                ? api.messages().sendOffline(uniqueId, "Lobby", "Welcome to the lobby")
                                : api.clients()
                                        .move(client.get().getId(), lobby)
                                        .thenCompose(done -> api.messages()
                                                .sendPrivate(client.get().getId(), "Welcome to the lobby")))
                        .thenApply(done -> lobby));
    }
    // workflow-async-end
}
