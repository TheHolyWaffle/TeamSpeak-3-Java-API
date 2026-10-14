package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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
