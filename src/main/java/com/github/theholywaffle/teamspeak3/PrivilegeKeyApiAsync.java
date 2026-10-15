package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelGroupId;
import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.PrivilegeKeyType;
import com.github.theholywaffle.teamspeak3.api.ServerGroupId;
import com.github.theholywaffle.teamspeak3.api.wrapper.PrivilegeKey;
import com.github.theholywaffle.teamspeak3.commands.PrivilegeKeyCommands;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous privilege key administration.
 *
 * @see PrivilegeKeyApi the synchronous form and full documentation
 */
public final class PrivilegeKeyApiAsync {

    private final QueryRequests requests;

    PrivilegeKeyApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see PrivilegeKeyApi#list() */
    public CompletableFuture<List<PrivilegeKey>> list() {
        return requests.models(PrivilegeKeyCommands.privilegeKeyList(), PrivilegeKey::new);
    }

    /** @see PrivilegeKeyApi#createForServerGroup(ServerGroupId, String) */
    public CompletableFuture<String> createForServerGroup(ServerGroupId serverGroupId, String description) {
        return requests.string(
                PrivilegeKeyCommands.privilegeKeyAdd(
                        PrivilegeKeyType.SERVER_GROUP, serverGroupId.value(), 0, description),
                "token");
    }

    /** @see PrivilegeKeyApi#createForChannelGroup(ChannelGroupId, ChannelId, String) */
    public CompletableFuture<String> createForChannelGroup(
            ChannelGroupId channelGroupId, ChannelId channelId, String description) {
        return requests.string(
                PrivilegeKeyCommands.privilegeKeyAdd(
                        PrivilegeKeyType.CHANNEL_GROUP, channelGroupId.value(), channelId.value(), description),
                "token");
    }

    /** @see PrivilegeKeyApi#delete(String) */
    public CompletableFuture<Void> delete(String token) {
        return requests.run(PrivilegeKeyCommands.privilegeKeyDelete(token));
    }

    /** @see PrivilegeKeyApi#use(String) */
    public CompletableFuture<Void> use(String token) {
        return requests.run(PrivilegeKeyCommands.privilegeKeyUse(token));
    }
}
