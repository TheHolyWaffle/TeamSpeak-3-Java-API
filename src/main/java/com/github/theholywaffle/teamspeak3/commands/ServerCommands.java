package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ServerInstanceProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import java.util.Map;

public final class ServerCommands {

    private ServerCommands() {
        throw new Error("No instances");
    }

    public static Command bindingList() {
        return new CommandBuilder("bindinglist").build();
    }

    public static Command gm(String message) {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message must be a non-empty string");
        }

        return new CommandBuilder("gm").add(new KeyValueParam("msg", message)).build();
    }

    public static Command hostInfo() {
        return new CommandBuilder("hostinfo").build();
    }

    public static Command instanceInfo() {
        return new CommandBuilder("instanceinfo").build();
    }

    public static Command instanceEdit(Map<ServerInstanceProperty, String> options) {
        return new CommandBuilder("instanceedit").addProperties(options).build();
    }

    public static Command logView(int lines, boolean instance) {
        if (lines > 100) throw new IllegalArgumentException("Can only fetch up to 100 lines at once (" + lines + ")");

        CommandBuilder builder = new CommandBuilder("logview");
        builder.addIf(lines > 0, new KeyValueParam("lines", lines));
        builder.addIf(instance, new KeyValueParam("instance", 1));
        return builder.build();
    }

    public static Command serverProcessStop(String reason) {
        CommandBuilder builder = new CommandBuilder("serverprocessstop");
        builder.addIf(reason != null, new KeyValueParam("reasonmsg", reason));
        return builder.build();
    }

    public static Command version() {
        return new CommandBuilder("version").build();
    }
}
