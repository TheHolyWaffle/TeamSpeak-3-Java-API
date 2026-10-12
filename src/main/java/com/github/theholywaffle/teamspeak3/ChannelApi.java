package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.ChannelInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Channel administration on the selected virtual server.
 * <p>
 * ID-based reads fail with the server's error (768 for an unknown channel); name lookups
 * return an empty {@link java.util.Optional} or list when nothing matches.
 * </p>
 *
 * @see ChannelApiAsync the asynchronous form
 */
public final class ChannelApi {

	private final ChannelApiAsync async;

	ChannelApi(ChannelApiAsync async) {
		this.async = async;
	}

	/**
	 * Lists all channels.
	 *
	 * @return all channels, in server order
	 */
	public List<Channel> list() {
		return CommandFutures.join(async.list());
	}

	/**
	 * Gets detailed information about one channel.
	 *
	 * @param channelId the channel ID
	 * @return the channel information
	 * @throws com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException with error 768 if the channel does not exist
	 */
	public ChannelInfo info(int channelId) {
		return CommandFutures.join(async.info(channelId));
	}

	/**
	 * Finds the first channel whose complete name equals {@code name}.
	 *
	 * @param name the complete channel name
	 * @param ignoreCase whether to compare case-insensitively
	 * @return the channel, or empty if no channel has that name
	 */
	public Optional<Channel> findByName(String name, boolean ignoreCase) {
		return CommandFutures.join(async.findByName(name, ignoreCase));
	}

	/**
	 * Finds channels whose name contains {@code namePart}.
	 *
	 * @param namePart a case-insensitive part of the channel name
	 * @return the matching channels; empty if none match
	 */
	public List<Channel> search(String namePart) {
		return CommandFutures.join(async.search(namePart));
	}

	/**
	 * Creates a channel.
	 *
	 * @param name the channel name
	 * @param properties further channel properties, for example parent, type or password; may be empty
	 * @return the ID of the new channel
	 */
	public int create(String name, Map<ChannelProperty, String> properties) {
		return CommandFutures.join(async.create(name, properties));
	}

	/**
	 * Changes channel properties. Use {@code Map.of(property, value)} for a single property.
	 *
	 * @param channelId the channel ID
	 * @param properties the properties to change
	 */
	public void edit(int channelId, Map<ChannelProperty, String> properties) {
		CommandFutures.join(async.edit(channelId, properties));
	}

	/**
	 * Deletes a channel, moving any clients in it to the default channel.
	 *
	 * @param channelId the channel ID
	 */
	public void delete(int channelId) {
		CommandFutures.join(async.delete(channelId));
	}

	/**
	 * Deletes a channel.
	 *
	 * @param channelId the channel ID
	 * @param force whether to delete a channel that still contains clients
	 */
	public void delete(int channelId, boolean force) {
		CommandFutures.join(async.delete(channelId, force));
	}

	/**
	 * Moves a channel to become the first child of another channel.
	 *
	 * @param channelId the channel to move
	 * @param parentChannelId the new parent channel, or 0 for the top level
	 */
	public void move(int channelId, int parentChannelId) {
		CommandFutures.join(async.move(channelId, parentChannelId));
	}

	/**
	 * Moves a channel below a parent channel and sibling.
	 *
	 * @param channelId the channel to move
	 * @param parentChannelId the new parent channel, or 0 for the top level
	 * @param order the ID of the sibling to place it after, or 0 to make it the first child
	 */
	public void move(int channelId, int parentChannelId, int order) {
		CommandFutures.join(async.move(channelId, parentChannelId, order));
	}
}
