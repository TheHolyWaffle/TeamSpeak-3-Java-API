package com.github.theholywaffle.teamspeak3.commands;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2017 Bert De Geyter, Roger Baumgartner
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;

public final class FileCommands {

	private FileCommands() {
		throw new Error("No instances");
	}

	public static Command ftCreateDir(String path, int channelId, String channelPassword) {
		if (path == null) throw new IllegalArgumentException("File path cannot be null");

		CommandBuilder builder = new CommandBuilder("ftcreatedir", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cpw", channelPassword));
		builder.add(new KeyValueParam("dirname", path.startsWith("/") ? path : "/" + path));
		return builder.build();
	}

	public static Command ftDeleteFile(int channelId, String channelPassword, String... filePaths) {
		if (filePaths == null || filePaths.length == 0) {
			throw new IllegalArgumentException("File array cannot be null or empty");
		}

		CommandBuilder builder = new CommandBuilder("ftdeletefile", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cpw", channelPassword));

		ArrayParameter files = new ArrayParameter(filePaths.length);
		for (String filePath : filePaths) {
			if (filePath == null) throw new IllegalArgumentException("File path cannot be null");
			files.add(new KeyValueParam("name", filePath.startsWith("/") ? filePath : "/" + filePath));
		}
		builder.add(files);

		return builder.build();
	}

	public static Command ftGetFileInfo(int channelId, String channelPassword, String... filePaths) {
		if (filePaths == null || filePaths.length == 0) {
			throw new IllegalArgumentException("File array cannot be null or empty");
		}

		CommandBuilder builder = new CommandBuilder("ftgetfileinfo", 1);
		ArrayParameter files = new ArrayParameter(filePaths.length, 3);
		for (String filePath : filePaths) {
			if (filePath == null) throw new IllegalArgumentException("File path cannot be null");
			files.add(new KeyValueParam("cid", channelId));
			files.add(new KeyValueParam("cpw", channelPassword));
			files.add(new KeyValueParam("name", filePath));
		}
		builder.add(files);

		return builder.build();
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

		CommandBuilder builder = new CommandBuilder("ftgetfileinfo", 1);
		ArrayParameter files = new ArrayParameter(filePaths.length, 3);
		for (int i = 0; i < filePaths.length; ++i) {
			final String password = channelPasswords == null ? null : channelPasswords[i];
			final String path = filePaths[i];
			if (path == null) throw new IllegalArgumentException("File path cannot be null");

			files.add(new KeyValueParam("cid", channelIds[i]));
			files.add(new KeyValueParam("cpw", password));
			files.add(new KeyValueParam("name", path.startsWith("/") ? path : "/" + path));
		}
		builder.add(files);

		return builder.build();
	}

	public static Command ftGetFileList(String directoryPath, int channelId, String channelPassword) {
		if (directoryPath == null) throw new IllegalArgumentException("Directory path cannot be null");

		CommandBuilder builder = new CommandBuilder("ftgetfilelist", 3);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cpw", channelPassword));

		String path = directoryPath; // Make sure path starts and ends with /
		if (!path.startsWith("/")) path = "/" + path;
		if (!path.endsWith("/")) path += "/";
		builder.add(new KeyValueParam("path", path));

		return builder.build();
	}

	public static Command ftInitDownload(int transferId, String path, int channelId, String channelPassword) {
		if (path == null) throw new IllegalArgumentException("File path cannot be null");

		CommandBuilder builder = new CommandBuilder("ftinitdownload", 6);
		builder.add(new KeyValueParam("clientftfid", transferId));
		builder.add(new KeyValueParam("name", path.startsWith("/") ? path : "/" + path));
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cpw", channelPassword));
		builder.add(new KeyValueParam("seekpos", 0));
		builder.add(new KeyValueParam("proto", 0)); // Use current (old) protocol for as long as possible
		return builder.build();
	}

	public static Command ftInitUpload(int transferId, String path, int channelId, String channelPassword,
	                                   long size, boolean overwrite) {
		if (path == null) throw new IllegalArgumentException("File path cannot be null");

		CommandBuilder builder = new CommandBuilder("ftinitupload", 8);
		builder.add(new KeyValueParam("clientftfid", transferId));
		builder.add(new KeyValueParam("name", path.startsWith("/") ? path : "/" + path));
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cpw", channelPassword));
		builder.add(new KeyValueParam("size", size));
		builder.add(new KeyValueParam("overwrite", overwrite));
		builder.add(new KeyValueParam("resume", 0));
		builder.add(new KeyValueParam("proto", 0)); // Use current (old) protocol for as long as possible
		return builder.build();
	}

	public static Command ftList() {
		return new CommandBuilder("ftlist").build();
	}

	public static Command ftRenameFile(String oldPath, String newPath,
	                                   int channelId, String channelPassword) {
		if (oldPath == null) throw new IllegalArgumentException("Old file path cannot be null");
		if (newPath == null) throw new IllegalArgumentException("New file path cannot be null");

		CommandBuilder builder = new CommandBuilder("ftrenamefile", 4);
		builder.add(new KeyValueParam("cid", channelId));
		builder.add(new KeyValueParam("cpw", channelPassword));
		builder.add(new KeyValueParam("oldname", oldPath.startsWith("/") ? oldPath : "/" + oldPath));
		builder.add(new KeyValueParam("newname", newPath.startsWith("/") ? newPath : "/" + newPath));
		return builder.build();
	}

	public static Command ftRenameFile(String oldPath, String newPath,
	                                   int oldChannelId, String oldChannelPassword,
	                                   int newChannelId, String newChannelPassword) {
		if (oldPath == null) throw new IllegalArgumentException("Old file path cannot be null");
		if (newPath == null) throw new IllegalArgumentException("New file path cannot be null");

		CommandBuilder builder = new CommandBuilder("ftrenamefile", 6);
		builder.add(new KeyValueParam("cid", oldChannelId));
		builder.add(new KeyValueParam("cpw", oldChannelPassword));
		builder.add(new KeyValueParam("tcid", newChannelId));
		builder.add(new KeyValueParam("tcpw", newChannelPassword));
		builder.add(new KeyValueParam("oldname", oldPath.startsWith("/") ? oldPath : "/" + oldPath));
		builder.add(new KeyValueParam("newname", newPath.startsWith("/") ? newPath : "/" + newPath));
		return builder.build();
	}
}
