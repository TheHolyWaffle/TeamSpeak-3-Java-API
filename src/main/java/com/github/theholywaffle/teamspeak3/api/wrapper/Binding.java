package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Objects;

/**
 * An address the ServerQuery interface is bound to, from {@code bindinglist}.
 *
 * @param ip the bound IP address
 */
public record Binding(String ip) {

    public Binding {
        Objects.requireNonNull(ip, "ip");
    }

    /** @throws com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException if the row lacks {@code ip} */
    public static Binding from(Wrapper row) {
        return new Binding(row.require("ip"));
    }
}
