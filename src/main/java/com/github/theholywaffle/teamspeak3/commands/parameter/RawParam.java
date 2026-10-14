package com.github.theholywaffle.teamspeak3.commands.parameter;

/**
 * Text sent unchanged.
 * @param raw already encoded text
 */
public record RawParam(String raw) implements Parameter {

    @Override
    public void appendTo(StringBuilder line) {
        line.append(raw);
    }
}
