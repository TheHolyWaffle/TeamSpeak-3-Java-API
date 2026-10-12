package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.UploadMode;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileListEntry;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransfer;
import com.github.theholywaffle.teamspeak3.api.wrapper.IconFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * File repository access, file transfers and icons.
 * <p>
 * Transfers use a separate plaintext TCP socket, also when the query uses SSH. Cancelling the
 * original asynchronous result closes that socket; caller streams stay open and unflushed.
 * {@link FileTransferConfig} bounds socket I/O and in-memory downloads. A missing file fails
 * with server error 2051 or 2052.
 * </p>
 *
 * @see FileApiAsync the asynchronous form
 */
public final class FileApi {

	private final FileApiAsync async;

	FileApi(FileApiAsync async) {
		this.async = async;
	}

	/**
	 * Lists the files and directories in a directory.
	 *
	 * @param directory the directory, for example {@code FileLocation.of(channelId, "/")}
	 * @return the directory entries; empty if the directory is empty
	 */
	public List<FileListEntry> list(FileLocation directory) {
		return CommandFutures.join(async.list(directory));
	}

	/**
	 * Gets information about one file.
	 *
	 * @param file the file
	 * @return the file information
	 * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException with error 2052 if the file does not exist
	 */
	public FileInfo info(FileLocation file) {
		return CommandFutures.join(async.info(file));
	}

	/**
	 * Gets information about several files, possibly in different channels, with one command.
	 *
	 * @param files the files
	 * @return the file information, in server order
	 */
	public List<FileInfo> info(List<FileLocation> files) {
		return CommandFutures.join(async.info(files));
	}

	/**
	 * Creates a directory.
	 *
	 * @param directory the directory to create
	 */
	public void createDirectory(FileLocation directory) {
		CommandFutures.join(async.createDirectory(directory));
	}

	/**
	 * Deletes a file or directory.
	 *
	 * @param file the file or directory
	 */
	public void delete(FileLocation file) {
		CommandFutures.join(async.delete(file));
	}

	/**
	 * Deletes several files or directories in one channel with one command.
	 *
	 * @param files the files; all must share a channel and password
	 * @throws IllegalArgumentException if the list is empty or spans channels or passwords
	 */
	public void delete(List<FileLocation> files) {
		CommandFutures.join(async.delete(files));
	}

	/**
	 * Renames or moves a file, possibly into another channel.
	 *
	 * @param from the current location
	 * @param to the new location
	 */
	public void move(FileLocation from, FileLocation to) {
		CommandFutures.join(async.move(from, to));
	}

	/**
	 * Lists the file transfers currently running on the server.
	 *
	 * @return the active transfers
	 */
	public List<FileTransfer> transfers() {
		return CommandFutures.join(async.transfers());
	}

	/**
	 * Uploads exactly {@code length} bytes from a stream. The stream is read but not closed.
	 *
	 * @param target the file to write
	 * @param data the content source
	 * @param length the number of bytes to upload
	 * @param mode whether to create, overwrite or resume the file
	 * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException with error 2050 if the file exists in {@link UploadMode#CREATE} mode
	 * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3FileTransferFailedException if the file socket or stream fails
	 */
	public void upload(FileLocation target, InputStream data, long length, UploadMode mode) {
		CommandFutures.join(async.upload(target, data, length, mode));
	}

	/**
	 * Uploads a byte array.
	 *
	 * @param target the file to write
	 * @param data the content
	 * @param mode whether to create, overwrite or resume the file
	 */
	public void upload(FileLocation target, byte[] data, UploadMode mode) {
		CommandFutures.join(async.upload(target, data, mode));
	}

	/**
	 * Downloads a file into a stream. The stream is neither flushed nor closed.
	 *
	 * @param file the file
	 * @param out the destination
	 * @return the number of bytes written
	 * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3FileTransferFailedException if the file socket or stream fails
	 */
	public long download(FileLocation file, OutputStream out) {
		return CommandFutures.join(async.download(file, out));
	}

	/**
	 * Downloads the remainder of a file from an offset into a stream.
	 *
	 * @param file the file
	 * @param out the destination
	 * @param offset the number of bytes to skip
	 * @return the number of bytes written
	 */
	public long download(FileLocation file, OutputStream out, long offset) {
		return CommandFutures.join(async.download(file, out, offset));
	}

	/**
	 * Downloads a file into memory, bounded by {@link FileTransferConfig#maxInMemoryBytes()}.
	 *
	 * @param file the file
	 * @return the file content
	 */
	public byte[] downloadBytes(FileLocation file) {
		return CommandFutures.join(async.downloadBytes(file));
	}

	/**
	 * Lists the icons uploaded to the selected virtual server.
	 *
	 * @return the icons
	 */
	public List<IconFile> icons() {
		return CommandFutures.join(async.icons());
	}

	/**
	 * Uploads an icon. The stream is buffered in memory to compute the icon ID. An existing identical icon is not an error.
	 *
	 * @param data the PNG or other image content
	 * @param length the number of bytes to read
	 * @return the icon ID
	 */
	public long uploadIcon(InputStream data, long length) {
		return CommandFutures.join(async.uploadIcon(data, length));
	}

	/**
	 * Uploads an icon. An existing identical icon is not an error.
	 *
	 * @param data the image content
	 * @return the icon ID
	 */
	public long uploadIcon(byte[] data) {
		return CommandFutures.join(async.uploadIcon(data));
	}

	/**
	 * Downloads an icon into a stream.
	 *
	 * @param iconId the icon ID
	 * @param out the destination
	 * @return the number of bytes written
	 */
	public long downloadIcon(long iconId, OutputStream out) {
		return CommandFutures.join(async.downloadIcon(iconId, out));
	}

	/**
	 * Downloads an icon into memory.
	 *
	 * @param iconId the icon ID
	 * @return the icon content
	 */
	public byte[] downloadIcon(long iconId) {
		return CommandFutures.join(async.downloadIcon(iconId));
	}

	/**
	 * Deletes icons.
	 *
	 * @param iconIds at least one icon ID
	 */
	public void deleteIcons(long... iconIds) {
		CommandFutures.join(async.deleteIcons(iconIds));
	}
}
