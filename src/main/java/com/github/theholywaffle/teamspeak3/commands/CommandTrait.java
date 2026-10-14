package com.github.theholywaffle.teamspeak3.commands;

import java.util.EnumSet;
import java.util.Set;

/**
 * What a ServerQuery command depends on or changes beyond its own request. Transports and the command
 * queue decide from these traits, never from command names.
 */
public enum CommandTrait {
    /** Changes per-session state that a reconnect would have to restore: login, server selection, nickname, notifications. */
    SESSION_STATE,
    /** Only meaningful on a persistent connection: login, logout, server selection and quit. */
    CONNECTION,
    /** Registers or removes server notifications. */
    NOTIFICATIONS,
    /** Uses the file repository or its transfer channel. */
    FILES,
    /** Reads ServerQuery command help. */
    HELP;

    /**
     * The single definition of each command's traits, shared by built and raw commands.
     *
     * @param commandName the protocol command name
     * @return the command's traits, empty for an ordinary request
     */
    static Set<CommandTrait> of(String commandName) {
        return switch (commandName) {
            case "login", "logout", "use" -> EnumSet.of(SESSION_STATE, CONNECTION);
            case "quit" -> EnumSet.of(CONNECTION);
            case "clientupdate" -> EnumSet.of(SESSION_STATE);
            case "servernotifyregister", "servernotifyunregister" -> EnumSet.of(SESSION_STATE, NOTIFICATIONS);
            case "help" -> EnumSet.of(HELP);
            default -> commandName.startsWith("ft") ? EnumSet.of(FILES) : EnumSet.noneOf(CommandTrait.class);
        };
    }
}
