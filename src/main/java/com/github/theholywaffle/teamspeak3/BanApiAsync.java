package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.BanId;
import com.github.theholywaffle.teamspeak3.api.BanRequest;
import com.github.theholywaffle.teamspeak3.api.ClientBan;
import com.github.theholywaffle.teamspeak3.api.wrapper.Ban;
import com.github.theholywaffle.teamspeak3.commands.BanCommands;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous ban rule administration.
 *
 * @see BanApi the synchronous form and full documentation
 */
public final class BanApiAsync {

    private final QueryRequests requests;

    BanApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see BanApi#list() */
    public CompletableFuture<List<Ban>> list() {
        return requests.models(BanCommands.banList(), Ban::new);
    }

    /** @see BanApi#add(BanRequest) */
    public CompletableFuture<BanId> add(BanRequest ban) {
        return requests.id(
                BanCommands.banAdd(
                        ban.ip(),
                        ban.name(),
                        ban.uniqueId(),
                        ban.myTeamSpeakId(),
                        ban.duration().toSeconds(),
                        ban.reason()),
                "banid",
                BanId::new);
    }

    /** @see BanApi#banClients(ClientBan) */
    public CompletableFuture<List<BanId>> banClients(ClientBan ban) {
        int[] ids = QueryRequests.values(ban.clientIds());
        return requests.ids(
                BanCommands.banClient(ids, ban.duration().toSeconds(), ban.reason(), ban.continueOnError()),
                "banid",
                BanId::new);
    }

    /** @see BanApi#delete(BanId) */
    public CompletableFuture<Void> delete(BanId banId) {
        return requests.run(BanCommands.banDel(banId.value()));
    }

    /** @see BanApi#deleteAll() */
    public CompletableFuture<Void> deleteAll() {
        return requests.run(BanCommands.banDelAll());
    }
}
