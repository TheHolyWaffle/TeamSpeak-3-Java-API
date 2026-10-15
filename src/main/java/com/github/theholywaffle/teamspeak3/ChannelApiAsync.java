package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;
import com.github.theholywaffle.teamspeak3.commands.ChannelCommands;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous channel administration on the selected virtual server.
 *
 * @see ChannelApi the synchronous form and full documentation
 */
public final class ChannelApiAsync {

    private final QueryRequests requests;

    ChannelApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see ChannelApi#list() */
    public CompletableFuture<List<Channel>> list() {
        return requests.models(ChannelCommands.channelList(), Channel::new);
    }

    /** @see ChannelApi#info(ChannelId) */
    public CompletableFuture<ChannelInfo> info(ChannelId channelId) {
        return requests.model(ChannelCommands.channelInfo(channelId.value()), map -> new ChannelInfo(channelId, map));
    }

    /** @see ChannelApi#findByName(String, boolean) */
    public CompletableFuture<Optional<Channel>> findByName(String name, boolean ignoreCase) {
        Objects.requireNonNull(name, "name");
        return CommandFutures.map(
                list(), channels -> QueryRequests.firstNamed(channels, Channel::getName, name, ignoreCase));
    }

    /** @see ChannelApi#search(String) */
    public CompletableFuture<List<Channel>> search(String namePart) {
        var ids = requests.ids(ChannelCommands.channelFind(namePart), "cid", ChannelId::new);
        return QueryRequests.recover(
                QueryRequests.findByKey(ids, list(), Channel::getId), QueryRequests.INVALID_CHANNEL_ID, List.of());
    }

    /** @see ChannelApi#create(String, Map) */
    public CompletableFuture<ChannelId> create(String name, Map<ChannelProperty, String> properties) {
        return requests.id(ChannelCommands.channelCreate(name, properties), "cid", ChannelId::new);
    }

    /** @see ChannelApi#edit(ChannelId, Map) */
    public CompletableFuture<Void> edit(ChannelId channelId, Map<ChannelProperty, String> properties) {
        return requests.run(ChannelCommands.channelEdit(channelId.value(), properties));
    }

    /** @see ChannelApi#delete(ChannelId) */
    public CompletableFuture<Void> delete(ChannelId channelId) {
        return delete(channelId, true);
    }

    /** @see ChannelApi#delete(ChannelId, boolean) */
    public CompletableFuture<Void> delete(ChannelId channelId, boolean force) {
        return requests.run(ChannelCommands.channelDelete(channelId.value(), force));
    }

    /** @see ChannelApi#move(ChannelId, ChannelId) */
    public CompletableFuture<Void> move(ChannelId channelId, ChannelId parentChannelId) {
        return move(channelId, parentChannelId, null);
    }

    /** @see ChannelApi#move(ChannelId, ChannelId, ChannelId) */
    public CompletableFuture<Void> move(ChannelId channelId, ChannelId parentChannelId, ChannelId afterChannelId) {
        return requests.run(ChannelCommands.channelMove(
                channelId.value(),
                QueryRequests.valueOrZero(parentChannelId),
                QueryRequests.valueOrZero(afterChannelId)));
    }
}
