package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import com.github.theholywaffle.teamspeak3.api.VirtualServerProperty;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.RawParam;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class VirtualServerCommands {

    private VirtualServerCommands() {
        throw new Error("No instances");
    }

    public static Command serverCreate(String name, Map<VirtualServerProperty, String> options) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Server name must be a non-empty string");
        }

        CommandBuilder builder = new CommandBuilder("servercreate");
        builder.add(new KeyValueParam(VirtualServerProperty.VIRTUALSERVER_NAME.getName(), name));
        builder.addProperties(options);
        return builder.build();
    }

    public static Command serverDelete(VirtualServerId id) {
        return new CommandBuilder("serverdelete")
                .add(new KeyValueParam("sid", id))
                .build();
    }

    public static Command serverEdit(Map<VirtualServerProperty, String> options) {
        return new CommandBuilder("serveredit").addProperties(options).build();
    }

    public static Command serverIdGetByPort(int port) {
        return new CommandBuilder("serveridgetbyport")
                .add(new KeyValueParam("virtualserver_port", port))
                .build();
    }

    public static Command serverInfo() {
        return new CommandBuilder("serverinfo").build();
    }

    public static Command serverList() {
        CommandBuilder builder = new CommandBuilder("serverlist");
        builder.add(new OptionParam("uid"));
        builder.add(new OptionParam("all"));
        return builder.build();
    }

    public static Command serverRequestConnectionInfo() {
        return new CommandBuilder("serverrequestconnectioninfo").build();
    }

    public static Command serverSnapshotCreate() {
        return new CommandBuilder("serversnapshotcreate").build();
    }

    public static Command serverSnapshotDeploy(String snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            throw new IllegalArgumentException("Server snapshot must be a non-empty string");
        }

        return new CommandBuilder("serversnapshotdeploy")
                .add(new RawParam(snapshot))
                .build();
    }

    public static Command serverStart(VirtualServerId id) {
        return new CommandBuilder("serverstart")
                .add(new KeyValueParam("sid", id))
                .build();
    }

    public static Command serverStop(VirtualServerId id, String reason) {
        CommandBuilder builder = new CommandBuilder("serverstop");
        builder.add(new KeyValueParam("sid", id));
        builder.addIf(reason != null, new KeyValueParam("reasonmsg", reason));
        return builder.build();
    }

    public static Command serverTempPasswordAdd(
            String password,
            String description,
            Duration lifetime,
            Optional<ChannelId> targetChannel,
            Optional<String> targetChannelPassword) {
        requirePassword(password);
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(lifetime, "lifetime");
        if (lifetime.isNegative() || lifetime.isZero() || lifetime.getNano() != 0) {
            throw new IllegalArgumentException("Temporary password lifetime must be positive whole seconds");
        }
        return new CommandBuilder("servertemppasswordadd")
                .add(new KeyValueParam("pw", password))
                .add(new KeyValueParam("desc", description))
                .add(new KeyValueParam("duration", lifetime.getSeconds()))
                .add(KeyValueParam.idOrZero("tcid", targetChannel))
                .add(new KeyValueParam("tcpw", targetChannelPassword.orElse("")))
                .build();
    }

    public static Command serverTempPasswordDelete(String password) {
        requirePassword(password);
        return new CommandBuilder("servertemppassworddel")
                .add(new KeyValueParam("pw", password))
                .build();
    }

    public static Command serverTempPasswordList() {
        return new CommandBuilder("servertemppasswordlist").build();
    }

    private static void requirePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Temporary password must be a non-empty string");
        }
    }
}
