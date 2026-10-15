package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.ServerFileTransferId;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import java.util.List;

/** File commands; TeamSpeak addresses the virtual server's own files as channel {@code 0}. */
public final class FileCommands {

    private FileCommands() {
        throw new Error("No instances");
    }

    public static Command ftCreateDir(FileLocation directory) {
        CommandBuilder builder = new CommandBuilder("ftcreatedir");
        addChannel(builder, directory);
        builder.add(new KeyValueParam("dirname", prefixSlash(directory.path())));
        return builder.build();
    }

    /** @param files the files to delete, which must share a channel and password */
    public static Command ftDeleteFile(List<FileLocation> files) {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("At least one file is required");
        FileLocation first = files.getFirst();
        for (FileLocation file : files) {
            if (!file.channelId().equals(first.channelId())
                    || !file.channelPassword().equals(first.channelPassword())) {
                throw new IllegalArgumentException("Files deleted together must share a channel and password");
            }
        }

        CommandBuilder builder = new CommandBuilder("ftdeletefile");
        addChannel(builder, first);
        builder.add(ArrayParameter.of(
                "name", files.stream().map(file -> prefixSlash(file.path())).toList()));
        return builder.build();
    }

    public static Command ftGetFileInfo(List<FileLocation> files) {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("At least one file is required");
        return new CommandBuilder("ftgetfileinfo")
                .add(new ArrayParameter(
                        files.stream().map(FileCommands::fileEntry).toList()))
                .build();
    }

    private static List<Parameter> fileEntry(FileLocation file) {
        return List.of(
                channelId("cid", file),
                channelPassword("cpw", file),
                new KeyValueParam("name", prefixSlash(file.path())));
    }

    public static Command ftGetFileList(FileLocation directory) {
        CommandBuilder builder = new CommandBuilder("ftgetfilelist");
        addChannel(builder, directory);
        String path = prefixSlash(directory.path());
        builder.add(new KeyValueParam("path", path.endsWith("/") ? path : path + "/"));
        return builder.build();
    }

    public static Command ftInitDownload(int transferId, FileLocation file, long seekPosition) {
        if (seekPosition < 0) throw new IllegalArgumentException("Negative download seek position");
        CommandBuilder builder = new CommandBuilder("ftinitdownload");
        builder.add(new KeyValueParam("clientftfid", transferId));
        builder.add(new KeyValueParam("name", prefixSlash(file.path())));
        addChannel(builder, file);
        builder.add(new KeyValueParam("seekpos", seekPosition));
        builder.add(new KeyValueParam("proto", 0)); // Use current (old) protocol for as long as possible
        return builder.build();
    }

    public static Command ftInitUpload(
            int transferId, FileLocation file, long size, boolean overwrite, boolean resume) {
        if (size < 0) throw new IllegalArgumentException("Negative upload length");
        if (resume && overwrite) throw new IllegalArgumentException("Resume cannot overwrite a file");
        CommandBuilder builder = new CommandBuilder("ftinitupload");
        builder.add(new KeyValueParam("clientftfid", transferId));
        builder.add(new KeyValueParam("name", prefixSlash(file.path())));
        addChannel(builder, file);
        builder.add(new KeyValueParam("size", size));
        builder.add(new KeyValueParam("overwrite", overwrite));
        builder.add(new KeyValueParam("resume", resume));
        builder.add(new KeyValueParam("proto", 0)); // Use current (old) protocol for as long as possible
        return builder.build();
    }

    public static Command ftStop(ServerFileTransferId transferId, boolean delete) {
        return new CommandBuilder("ftstop")
                .add(new KeyValueParam("serverftfid", transferId.value()))
                .add(new KeyValueParam("delete", delete))
                .build();
    }

    public static Command ftList() {
        return new CommandBuilder("ftlist").build();
    }

    /** Moves within one channel, or across channels when {@code to} names another channel. */
    public static Command ftRenameFile(FileLocation from, FileLocation to) {
        CommandBuilder builder = new CommandBuilder("ftrenamefile");
        addChannel(builder, from);
        if (!from.channelId().equals(to.channelId())) {
            builder.add(channelId("tcid", to));
            builder.add(channelPassword("tcpw", to));
        }
        builder.add(new KeyValueParam("oldname", prefixSlash(from.path())));
        builder.add(new KeyValueParam("newname", prefixSlash(to.path())));
        return builder.build();
    }

    private static void addChannel(CommandBuilder builder, FileLocation location) {
        builder.add(channelId("cid", location));
        builder.add(channelPassword("cpw", location));
    }

    private static KeyValueParam channelPassword(String key, FileLocation location) {
        return new KeyValueParam(key, location.channelPassword().orElse(""));
    }

    private static KeyValueParam channelId(String key, FileLocation location) {
        return KeyValueParam.idOrZero(key, location.channelId());
    }

    private static String prefixSlash(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }
}
