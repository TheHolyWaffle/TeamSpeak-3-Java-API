package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public final class FileCommands {

    private FileCommands() {
        throw new Error("No instances");
    }

    public static Command ftCreateDir(String path, int channelId, String channelPassword) {
        CommandBuilder builder = new CommandBuilder("ftcreatedir");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cpw", channelPassword));
        builder.add(new KeyValueParam("dirname", prefixSlash(path)));
        return builder.build();
    }

    public static Command ftDeleteFile(int channelId, String channelPassword, String... filePaths) {
        if (filePaths == null || filePaths.length == 0) {
            throw new IllegalArgumentException("File array cannot be null or empty");
        }

        CommandBuilder builder = new CommandBuilder("ftdeletefile");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cpw", channelPassword));

        builder.add(ArrayParameter.of(
                "name", Arrays.stream(filePaths).map(FileCommands::prefixSlash).toList()));
        return builder.build();
    }

    public static Command ftGetFileInfo(int channelId, String channelPassword, String... filePaths) {
        if (filePaths == null || filePaths.length == 0) {
            throw new IllegalArgumentException("File array cannot be null or empty");
        }

        return ftGetFileInfo(Arrays.stream(filePaths)
                .map(path -> fileEntry(channelId, channelPassword, path))
                .toList());
    }

    public static Command ftGetFileInfo(int[] channelIds, String[] channelPasswords, String[] filePaths) {
        if (channelIds == null || channelIds.length == 0) {
            throw new IllegalArgumentException("Channel ID array cannot be null or empty");
        }
        if (filePaths == null || filePaths.length == 0) {
            throw new IllegalArgumentException("File array cannot be null or empty");
        }
        if (channelIds.length != filePaths.length) {
            throw new IllegalArgumentException("Channel IDs length doesn't match file paths length");
        }
        if (channelPasswords != null && filePaths.length != channelPasswords.length) {
            throw new IllegalArgumentException("Passwords length doesn't match file paths length");
        }

        return ftGetFileInfo(IntStream.range(0, filePaths.length)
                .mapToObj(i ->
                        fileEntry(channelIds[i], channelPasswords == null ? null : channelPasswords[i], filePaths[i]))
                .toList());
    }

    private static Command ftGetFileInfo(List<List<Parameter>> files) {
        return new CommandBuilder("ftgetfileinfo")
                .add(new ArrayParameter(files))
                .build();
    }

    private static List<Parameter> fileEntry(int channelId, String channelPassword, String path) {
        return List.of(
                new KeyValueParam("cid", channelId),
                new KeyValueParam("cpw", channelPassword),
                new KeyValueParam("name", prefixSlash(path)));
    }

    public static Command ftGetFileList(String directoryPath, int channelId, String channelPassword) {
        if (directoryPath == null) throw new IllegalArgumentException("Directory path cannot be null");

        CommandBuilder builder = new CommandBuilder("ftgetfilelist");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cpw", channelPassword));

        String path = directoryPath; // Make sure path starts and ends with /
        if (!path.startsWith("/")) path = "/" + path;
        if (!path.endsWith("/")) path += "/";
        builder.add(new KeyValueParam("path", path));

        return builder.build();
    }

    public static Command ftInitDownload(
            int transferId, String path, int channelId, String channelPassword, long seekPosition) {
        if (seekPosition < 0) throw new IllegalArgumentException("Negative download seek position");
        CommandBuilder builder = new CommandBuilder("ftinitdownload");
        builder.add(new KeyValueParam("clientftfid", transferId));
        builder.add(new KeyValueParam("name", prefixSlash(path)));
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cpw", channelPassword));
        builder.add(new KeyValueParam("seekpos", seekPosition));
        builder.add(new KeyValueParam("proto", 0)); // Use current (old) protocol for as long as possible
        return builder.build();
    }

    public static Command ftInitUpload(
            int transferId,
            String path,
            int channelId,
            String channelPassword,
            long size,
            boolean overwrite,
            boolean resume) {
        if (size < 0) throw new IllegalArgumentException("Negative upload length");
        if (resume && overwrite) throw new IllegalArgumentException("Resume cannot overwrite a file");
        CommandBuilder builder = new CommandBuilder("ftinitupload");
        builder.add(new KeyValueParam("clientftfid", transferId));
        builder.add(new KeyValueParam("name", prefixSlash(path)));
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cpw", channelPassword));
        builder.add(new KeyValueParam("size", size));
        builder.add(new KeyValueParam("overwrite", overwrite));
        builder.add(new KeyValueParam("resume", resume));
        builder.add(new KeyValueParam("proto", 0)); // Use current (old) protocol for as long as possible
        return builder.build();
    }

    public static Command ftList() {
        return new CommandBuilder("ftlist").build();
    }

    public static Command ftRenameFile(String oldPath, String newPath, int channelId, String channelPassword) {
        if (oldPath == null) throw new IllegalArgumentException("Old file path cannot be null");
        if (newPath == null) throw new IllegalArgumentException("New file path cannot be null");

        CommandBuilder builder = new CommandBuilder("ftrenamefile");
        builder.add(new KeyValueParam("cid", channelId));
        builder.add(new KeyValueParam("cpw", channelPassword));
        builder.add(new KeyValueParam("oldname", prefixSlash(oldPath)));
        builder.add(new KeyValueParam("newname", prefixSlash(newPath)));
        return builder.build();
    }

    public static Command ftRenameFile(
            String oldPath,
            String newPath,
            int oldChannelId,
            String oldChannelPassword,
            int newChannelId,
            String newChannelPassword) {
        if (oldPath == null) throw new IllegalArgumentException("Old file path cannot be null");
        if (newPath == null) throw new IllegalArgumentException("New file path cannot be null");

        CommandBuilder builder = new CommandBuilder("ftrenamefile");
        builder.add(new KeyValueParam("cid", oldChannelId));
        builder.add(new KeyValueParam("cpw", oldChannelPassword));
        builder.add(new KeyValueParam("tcid", newChannelId));
        builder.add(new KeyValueParam("tcpw", newChannelPassword));
        builder.add(new KeyValueParam("oldname", prefixSlash(oldPath)));
        builder.add(new KeyValueParam("newname", prefixSlash(newPath)));
        return builder.build();
    }

    private static String prefixSlash(String path) {
        if (path == null) throw new IllegalArgumentException("File path cannot be null");
        return path.startsWith("/") ? path : "/" + path;
    }
}
