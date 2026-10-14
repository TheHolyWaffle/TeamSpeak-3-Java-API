package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.commands.QueryCommands;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous API of a RAW or SSH {@link TS3Query}: the shared {@link TS3BaseApiAsync} plus the
 * ServerQuery session, login and virtual server selection.
 *
 * @see TS3Api The synchronous version of the API
 */
public class TS3ApiAsync extends TS3BaseApiAsync {

    TS3ApiAsync(QueryBackend backend) {
        super(backend);
    }

    /** @see TS3Api#login(String, String) */
    public CompletableFuture<Void> login(String username, String password) {
        return requests.run(QueryCommands.logIn(username, password));
    }

    /** @see TS3Api#logout() */
    public CompletableFuture<Void> logout() {
        return requests.run(QueryCommands.logOut());
    }

    /** @see TS3Api#selectVirtualServerById(int) */
    public CompletableFuture<Void> selectVirtualServerById(int id) {
        return selectVirtualServerById(id, null);
    }

    /** @see TS3Api#selectVirtualServerById(int, String) */
    public CompletableFuture<Void> selectVirtualServerById(int id, String nickname) {
        return requests.run(QueryCommands.useId(id, nickname));
    }

    /** @see TS3Api#selectVirtualServerByPort(int) */
    public CompletableFuture<Void> selectVirtualServerByPort(int port) {
        return selectVirtualServerByPort(port, null);
    }

    /** @see TS3Api#selectVirtualServerByPort(int, String) */
    public CompletableFuture<Void> selectVirtualServerByPort(int port, String nickname) {
        return requests.run(QueryCommands.usePort(port, nickname));
    }
}
