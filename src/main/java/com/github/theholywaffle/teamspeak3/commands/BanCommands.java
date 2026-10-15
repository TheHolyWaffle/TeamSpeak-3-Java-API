package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;

public final class BanCommands {

    private BanCommands() {
        throw new Error("No instances");
    }

    public static Command banAdd(String ip, String name, String uid, String myTSId, long timeInSeconds, String reason) {
        if (ip == null && name == null && uid == null && myTSId == null) {
            throw new IllegalArgumentException("Either IP, name, UId or myTeamSpeak ID must be non-null");
        }

        CommandBuilder builder = new CommandBuilder("banadd");
        builder.addIf(ip != null, new KeyValueParam("ip", ip));
        builder.addIf(name != null, new KeyValueParam("name", name));
        builder.addIf(uid != null, new KeyValueParam("uid", uid));
        builder.addIf(myTSId != null, new KeyValueParam("mytsid", myTSId));
        builder.addIf(timeInSeconds > 0, new KeyValueParam("time", timeInSeconds));
        builder.addIf(reason != null, new KeyValueParam("banreason", reason));
        return builder.build();
    }

    public static Command banClient(int[] clientIds, long timeInSeconds, String reason, boolean continueOnError) {
        CommandBuilder builder = new CommandBuilder("banclient");
        builder.addIf(timeInSeconds > 0, new KeyValueParam("time", timeInSeconds));
        builder.addIf(reason != null, new KeyValueParam("banreason", reason));
        builder.addIf(continueOnError, new OptionParam("continueonerror"));

        builder.add(ArrayParameter.of("clid", clientIds));
        return builder.build();
    }

    public static Command banDel(long banId) {
        return new CommandBuilder("bandel")
                .add(new KeyValueParam("banid", banId))
                .build();
    }

    public static Command banDelAll() {
        return new CommandBuilder("bandelall").build();
    }

    public static Command banList() {
        return new CommandBuilder("banlist").build();
    }
}
