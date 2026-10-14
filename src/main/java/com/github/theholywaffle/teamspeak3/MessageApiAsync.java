package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.wrapper.Message;
import com.github.theholywaffle.teamspeak3.commands.ClientCommands;
import com.github.theholywaffle.teamspeak3.commands.MessageCommands;
import com.github.theholywaffle.teamspeak3.commands.ServerCommands;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous text messages and offline messages.
 *
 * @see MessageApi the synchronous form and full documentation
 */
public final class MessageApiAsync {

    private final QueryRequests requests;

    MessageApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see MessageApi#sendPrivate(ClientId, String) */
    public CompletableFuture<Void> sendPrivate(ClientId clientId, String message) {
        return send(TextMessageTargetMode.CLIENT, clientId.value(), message);
    }

    /** @see MessageApi#sendChannel(String) */
    public CompletableFuture<Void> sendChannel(String message) {
        return send(TextMessageTargetMode.CHANNEL, 0, message);
    }

    /** @see MessageApi#sendChannel(int, String) */
    public CompletableFuture<Void> sendChannel(int channelId, String message) {
        return CommandFutures.compose(
                requests.run(ClientCommands.clientMove(0, channelId, null)), ignored -> sendChannel(message));
    }

    /** @see MessageApi#sendServer(String) */
    public CompletableFuture<Void> sendServer(String message) {
        return send(TextMessageTargetMode.SERVER, 0, message);
    }

    /** @see MessageApi#sendServer(int, String) */
    public CompletableFuture<Void> sendServer(int serverId, String message) {
        return CommandFutures.compose(
                requests.backend().onVirtualServer(serverId),
                backend -> new MessageApiAsync(new QueryRequests(backend)).sendServer(message));
    }

    /** @see MessageApi#broadcast(String) */
    public CompletableFuture<Void> broadcast(String message) {
        return requests.run(ServerCommands.gm(message));
    }

    /** @see MessageApi#listOffline() */
    public CompletableFuture<List<Message>> listOffline() {
        return requests.models(MessageCommands.messageList(), Message::new);
    }

    /** @see MessageApi#getOffline(int) */
    public CompletableFuture<Optional<String>> getOffline(int messageId) {
        return requests.optional(
                MessageCommands.messageGet(messageId), row -> row.getMap().get("message"));
    }

    /** @see MessageApi#sendOffline(String, String, String) */
    public CompletableFuture<Void> sendOffline(String uniqueId, String subject, String message) {
        return requests.run(MessageCommands.messageAdd(uniqueId, subject, message));
    }

    /** @see MessageApi#deleteOffline(int) */
    public CompletableFuture<Void> deleteOffline(int messageId) {
        return requests.run(MessageCommands.messageDel(messageId));
    }

    /** @see MessageApi#setOfflineRead(int, boolean) */
    public CompletableFuture<Void> setOfflineRead(int messageId, boolean read) {
        return requests.run(MessageCommands.messageUpdateFlag(messageId, read));
    }

    private CompletableFuture<Void> send(TextMessageTargetMode mode, int targetId, String message) {
        return requests.run(ClientCommands.sendTextMessage(mode.getIndex(), targetId, message));
    }
}
