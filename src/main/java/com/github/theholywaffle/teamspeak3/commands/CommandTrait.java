package com.github.theholywaffle.teamspeak3.commands;

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
    /** Asks the server to close the connection after responding. */
    ENDS_CONNECTION,
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
     * @return the command's immutable traits, empty for an ordinary request
     */
    static Set<CommandTrait> of(String commandName) {
        return switch (commandName) {
            case "login", "logout", "use" -> SESSION_CONNECTION;
            case "quit" -> QUIT;
            case "clientupdate" -> SESSION;
            case "servernotifyregister", "servernotifyunregister" -> SESSION_NOTIFICATIONS;
            case "help" -> HELP_ONLY;
            default -> commandName.startsWith("ft") ? FILES_ONLY : Set.of();
        };
    }

    private static final Set<CommandTrait> SESSION_CONNECTION = Set.of(SESSION_STATE, CONNECTION);
    private static final Set<CommandTrait> QUIT = Set.of(CONNECTION, ENDS_CONNECTION);
    private static final Set<CommandTrait> SESSION = Set.of(SESSION_STATE);
    private static final Set<CommandTrait> SESSION_NOTIFICATIONS = Set.of(SESSION_STATE, NOTIFICATIONS);
    private static final Set<CommandTrait> HELP_ONLY = Set.of(HELP);
    private static final Set<CommandTrait> FILES_ONLY = Set.of(FILES);
}
