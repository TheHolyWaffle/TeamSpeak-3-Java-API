package com.github.theholywaffle.teamspeak3;

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

    /** @see ChannelApi#info(int) */
    public CompletableFuture<ChannelInfo> info(int channelId) {
        return requests.model(ChannelCommands.channelInfo(channelId), map -> new ChannelInfo(channelId, map));
    }

    /** @see ChannelApi#findByName(String, boolean) */
    public CompletableFuture<Optional<Channel>> findByName(String name, boolean ignoreCase) {
        Objects.requireNonNull(name, "name");
        return CommandFutures.map(
                list(), channels -> QueryRequests.firstNamed(channels, Channel::getName, name, ignoreCase));
    }

    /** @see ChannelApi#search(String) */
    public CompletableFuture<List<Channel>> search(String namePart) {
        var ids = requests.integers(ChannelCommands.channelFind(namePart), "cid");
        return QueryRequests.recover(
                QueryRequests.findByKey(ids, list(), Channel::getId), QueryRequests.INVALID_CHANNEL_ID, List.of());
    }

    /** @see ChannelApi#create(String, Map) */
    public CompletableFuture<Integer> create(String name, Map<ChannelProperty, String> properties) {
        return requests.createdId(ChannelCommands.channelCreate(name, properties), "cid");
    }

    /** @see ChannelApi#edit(int, Map) */
    public CompletableFuture<Void> edit(int channelId, Map<ChannelProperty, String> properties) {
        return requests.run(ChannelCommands.channelEdit(channelId, properties));
    }

    /** @see ChannelApi#delete(int) */
    public CompletableFuture<Void> delete(int channelId) {
        return delete(channelId, true);
    }

    /** @see ChannelApi#delete(int, boolean) */
    public CompletableFuture<Void> delete(int channelId, boolean force) {
        return requests.run(ChannelCommands.channelDelete(channelId, force));
    }

    /** @see ChannelApi#move(int, int) */
    public CompletableFuture<Void> move(int channelId, int parentChannelId) {
        return move(channelId, parentChannelId, 0);
    }

    /** @see ChannelApi#move(int, int, int) */
    public CompletableFuture<Void> move(int channelId, int parentChannelId, int order) {
        return requests.run(ChannelCommands.channelMove(channelId, parentChannelId, order));
    }
}
