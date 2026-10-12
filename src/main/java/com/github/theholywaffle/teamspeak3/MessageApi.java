package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.Message;

import java.util.List;
import java.util.Optional;

/**
 * Text messages to clients, channels and servers, and offline messages stored for database clients.
 *
 * @see MessageApiAsync the asynchronous form
 */
public final class MessageApi {

	private final MessageApiAsync async;

	MessageApi(MessageApiAsync async) {
		this.async = async;
	}

	/**
	 * Sends a private text message to a connected client.
	 *
	 * @param clientId the client ID
	 * @param message the message
	 */
	public void sendPrivate(ClientId clientId, String message) {
		CommandFutures.join(async.sendPrivate(clientId, message));
	}

	/**
	 * Sends a text message to this query client's current channel.
	 *
	 * @param message the message
	 */
	public void sendChannel(String message) {
		CommandFutures.join(async.sendChannel(message));
	}

	/**
	 * Moves this query client to a channel, then sends a text message to it.
	 *
	 * @param channelId the channel ID
	 * @param message the message
	 */
	public void sendChannel(int channelId, String message) {
		CommandFutures.join(async.sendChannel(channelId, message));
	}

	/**
	 * Sends a text message to the selected virtual server.
	 *
	 * @param message the message
	 */
	public void sendServer(String message) {
		CommandFutures.join(async.sendServer(message));
	}

	/**
	 * Selects a virtual server, then sends a text message to it. The server stays selected.
	 *
	 * @param serverId the virtual server ID
	 * @param message the message
	 */
	public void sendServer(int serverId, String message) {
		CommandFutures.join(async.sendServer(serverId, message));
	}

	/**
	 * Sends a text message to every client on every virtual server of the instance.
	 *
	 * @param message the message
	 */
	public void broadcast(String message) {
		CommandFutures.join(async.broadcast(message));
	}

	/**
	 * Lists the offline messages of this query client's database identity.
	 *
	 * @return the offline messages, without bodies
	 */
	public List<Message> listOffline() {
		return CommandFutures.join(async.listOffline());
	}

	/**
	 * Gets the body of an offline message.
	 *
	 * @param messageId the message ID
	 * @return the body, which may be empty; empty if no such message exists
	 */
	public Optional<String> getOffline(int messageId) {
		return CommandFutures.join(async.getOffline(messageId));
	}

	/**
	 * Stores an offline message for a database client.
	 *
	 * @param uniqueId the recipient's unique identifier
	 * @param subject the subject
	 * @param message the body
	 */
	public void sendOffline(String uniqueId, String subject, String message) {
		CommandFutures.join(async.sendOffline(uniqueId, subject, message));
	}

	/**
	 * Deletes an offline message.
	 *
	 * @param messageId the message ID
	 */
	public void deleteOffline(int messageId) {
		CommandFutures.join(async.deleteOffline(messageId));
	}

	/**
	 * Marks an offline message as read or unread.
	 *
	 * @param messageId the message ID
	 * @param read whether the message is read
	 */
	public void setOfflineRead(int messageId, boolean read) {
		CommandFutures.join(async.setOfflineRead(messageId, read));
	}
}
