package com.github.theholywaffle.teamspeak3.api.wrapper;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2016 Bert De Geyter, Roger Baumgartner
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

import java.util.Date;
import java.util.Map;

public class FileInfo extends Wrapper {

	public FileInfo(Map<String, String> map) {
		super(map);
	}

	/**
	 * Gets the ID of the channel this file is stored on.
	 *
	 * @return the ID of the file's channel
	 */
	public int getChannelId() {
		return getInt("cid");
	}

	/**
	 * Gets the path to this file or directory, including its name.
	 * This path can be used in other file transfer commands.
	 *
	 * @return the path to the file or directory
	 */
	public String getPath() {
		return get("name");
	}

	/**
	 * Gets the name of this file or directory.
	 *
	 * @return the name of this file or directory
	 */
	public String getName() {
		String fullPath = getPath();
		int slashPos = fullPath.lastIndexOf('/');
		if (slashPos < 0) return fullPath;
		return fullPath.substring(slashPos + 1);
	}

	/**
	 * Gets the path of the directory containing this file or directory.
	 *
	 * @return the path to the parent directory
	 *
	 * @see #getPath()
	 */
	public String getParentPath() {
		String fullPath = getPath();
		int slashPos = fullPath.lastIndexOf('/');
		if (slashPos < 0) return "/";
		return fullPath.substring(0, slashPos + 1);
	}

	/**
	 * Gets the size of the file in bytes.
	 * Note that this can return wrong values if the file is still being uploaded
	 * or if the upload has been paused by the uploading client.
	 *
	 * @return the file's size
	 */
	public long getFileSize() {
		return getLong("size");
	}

	/**
	 * Gets the date of the last modification to this file.
	 * The date has a one-second resolution.
	 *
	 * @return the file's last modification date
	 */
	public Date getLastModifiedDate() {
		return new Date(getLong("datetime") * 1000);
	}

	/**
	 * Gets whether this entry is a directory or a file.
	 * {@code 0} stands for a directory, {@code 1} for a file.
	 * <p>
	 * Consider using {@link #isFile} and {@link #isDirectory} instead.
	 * </p>
	 *
	 * @return the type of this file entry
	 */
	public int getType() {
		return 1;
	}

	/**
	 * Returns {@code true} if this entry is a file and not a directory.
	 *
	 * @return whether this is a file
	 */
	public boolean isFile() {
		return true;
	}

	/**
	 * Returns {@code true} if this entry is a directory and not a file.
	 *
	 * @return whether this is a directory
	 */
	public boolean isDirectory() {
		return false;
	}
}
