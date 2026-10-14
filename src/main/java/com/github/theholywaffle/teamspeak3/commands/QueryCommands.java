package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.RawParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.ValueParam;

public final class QueryCommands {

    private QueryCommands() {
        throw new Error("No instances");
    }

    /**
     * Builds one already-encoded ServerQuery command. Control characters are rejected
     * and the lower-case command name remains separate for session and retry policy checks.
     *
     * @param line one protocol command, with values encoded using {@link CommandEncoding#encode(String)}
     * @return the command
     * @throws IllegalArgumentException if the line is empty, has control characters or an invalid command name
     */
    public static Command rawCommand(String line) {
        if (line == null || line.isEmpty() || line.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Expected one encoded ServerQuery command without control characters");
        }
        int separator = line.indexOf(' ');
        String name = separator < 0 ? line : line.substring(0, separator);
        if (!name.matches("[a-z][a-z0-9]*")) {
            throw new IllegalArgumentException("ServerQuery command name must be lower-case ASCII letters and digits");
        }
        var builder = new CommandBuilder(name);
        if (separator >= 0) builder.add(new RawParam(line.substring(separator + 1)));
        return builder.build();
    }

    public static Command logIn(String username, String password) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username must be a non-empty string");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must be a non-empty string");
        }

        return new CommandBuilder("login")
                .add(new ValueParam(username))
                .add(new ValueParam(password))
                .build();
    }

    public static Command logOut() {
        return new CommandBuilder("logout").build();
    }

    public static Command quit() {
        return new CommandBuilder("quit").build();
    }

    public static Command serverNotifyRegister(TS3EventType eventType, int channelId) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }

        CommandBuilder builder = new CommandBuilder("servernotifyregister");
        builder.add(new KeyValueParam("event", eventType.toString()));
        builder.addIf(channelId >= 0, new KeyValueParam("id", channelId));
        return builder.build();
    }

    public static Command serverNotifyUnregister() {
        return new CommandBuilder("servernotifyunregister").build();
    }

    public static Command useId(int id, String nickname) {
        CommandBuilder builder = new CommandBuilder("use");
        builder.add(new KeyValueParam("sid", id));
        builder.add(new OptionParam("virtual"));
        builder.addIf(nickname != null, new KeyValueParam("client_nickname", nickname));
        return builder.build();
    }

    public static Command usePort(int port, String nickname) {
        CommandBuilder builder = new CommandBuilder("use");
        builder.add(new KeyValueParam("port", port));
        builder.add(new OptionParam("virtual"));
        builder.addIf(nickname != null, new KeyValueParam("client_nickname", nickname));
        return builder.build();
    }

    public static Command whoAmI() {
        return new CommandBuilder("whoami").build();
    }
}
