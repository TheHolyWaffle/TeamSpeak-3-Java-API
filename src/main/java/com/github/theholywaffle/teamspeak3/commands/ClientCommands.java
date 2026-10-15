package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.ChannelId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.ReasonIdentifier;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import java.util.Map;

public final class ClientCommands {

    private ClientCommands() {
        throw new Error("No instances");
    }

    public static Command clientEdit(ClientId clientId, Map<ClientProperty, String> options) {
        CommandBuilder builder = new CommandBuilder("clientedit");
        builder.add(new KeyValueParam("clid", clientId.value()));
        builder.addProperties(options);
        return builder.build();
    }

    public static Command clientFind(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Client name pattern must be a non-empty string");
        }

        return new CommandBuilder("clientfind")
                .add(new KeyValueParam("pattern", pattern))
                .build();
    }

    public static Command clientGetDBIdFromUId(String clientUId) {
        if (clientUId == null || clientUId.isEmpty()) {
            throw new IllegalArgumentException("Client UId must be a non-empty string");
        }

        return new CommandBuilder("clientgetdbidfromuid")
                .add(new KeyValueParam("cluid", clientUId))
                .build();
    }

    public static Command clientGetIds(String clientUId) {
        if (clientUId == null || clientUId.isEmpty()) {
            throw new IllegalArgumentException("Client UId must be a non-empty string");
        }

        return new CommandBuilder("clientgetids")
                .add(new KeyValueParam("cluid", clientUId))
                .build();
    }

    public static Command clientInfo(ClientId clientId) {
        return new CommandBuilder("clientinfo")
                .add(new KeyValueParam("clid", clientId.value()))
                .build();
    }

    public static Command clientKick(ReasonIdentifier reason, String reasonMessage, int... clientIds) {
        if (clientIds == null || clientIds.length == 0) {
            throw new IllegalArgumentException("Client ID array cannot be null or empty");
        }

        CommandBuilder builder = new CommandBuilder("clientkick");
        builder.add(new KeyValueParam("reasonid", reason.getIndex()));
        builder.addIf(reasonMessage != null, new KeyValueParam("reasonmsg", reasonMessage));

        builder.add(ArrayParameter.of("clid", clientIds));
        return builder.build();
    }

    public static Command clientList() {
        CommandBuilder builder = new CommandBuilder("clientlist");
        builder.add(new OptionParam("uid"));
        builder.add(new OptionParam("away"));
        builder.add(new OptionParam("voice"));
        builder.add(new OptionParam("times"));
        builder.add(new OptionParam("groups"));
        builder.add(new OptionParam("info"));
        builder.add(new OptionParam("icon"));
        builder.add(new OptionParam("country"));
        builder.add(new OptionParam("ip"));
        builder.add(new OptionParam("badges"));
        builder.add(new OptionParam("location"));
        return builder.build();
    }

    public static Command clientMove(ClientId clientId, ChannelId channelId, String channelPassword) {
        return clientMove(clientId.value(), channelId, channelPassword);
    }

    /** Moves the query client itself. */
    public static Command clientMoveSelf(ChannelId channelId, String channelPassword) {
        return clientMove(0, channelId, channelPassword);
    }

    private static Command clientMove(int clientId, ChannelId channelId, String channelPassword) {
        CommandBuilder builder = new CommandBuilder("clientmove");
        builder.add(new KeyValueParam("clid", clientId));
        builder.add(new KeyValueParam("cid", channelId));
        builder.addIf(channelPassword != null, new KeyValueParam("cpw", channelPassword));
        return builder.build();
    }

    public static Command clientMove(int[] clientIds, ChannelId channelId, String channelPassword) {
        if (clientIds == null || clientIds.length == 0) {
            throw new IllegalArgumentException("Client ID array cannot be null or empty");
        }

        CommandBuilder builder = new CommandBuilder("clientmove");
        builder.add(new KeyValueParam("cid", channelId));
        builder.addIf(channelPassword != null, new KeyValueParam("cpw", channelPassword));

        builder.add(ArrayParameter.of("clid", clientIds));
        return builder.build();
    }

    public static Command clientPoke(ClientId clientId, String message) {
        CommandBuilder builder = new CommandBuilder("clientpoke");
        builder.add(new KeyValueParam("clid", clientId.value()));
        builder.add(new KeyValueParam("msg", message));
        return builder.build();
    }

    public static Command clientSetServerQueryLogin(String username) {
        CommandBuilder builder = new CommandBuilder("clientsetserverquerylogin");
        builder.add(new KeyValueParam("client_login_name", username));
        return builder.build();
    }

    public static Command clientUpdate(Map<ClientProperty, String> options) {
        return new CommandBuilder("clientupdate").addProperties(options).build();
    }

    public static Command sendTextMessage(int targetMode, int targetId, String message) {
        CommandBuilder builder = new CommandBuilder("sendtextmessage");
        builder.add(new KeyValueParam("targetmode", targetMode));
        builder.add(new KeyValueParam("target", targetId));
        builder.add(new KeyValueParam("msg", message));
        return builder.build();
    }
}
