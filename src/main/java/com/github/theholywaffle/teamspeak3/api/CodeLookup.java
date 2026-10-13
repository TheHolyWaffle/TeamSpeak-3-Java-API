package com.github.theholywaffle.teamspeak3.api;

import java.util.function.ToIntFunction;

final class CodeLookup {

    private CodeLookup() {}

    static <E extends Enum<E>> E byIndex(E[] values, ToIntFunction<E> index, int code, E unknown) {
        for (E value : values) {
            if (value != unknown && index.applyAsInt(value) == code) return value;
        }
        return unknown;
    }
}
