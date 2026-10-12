package com.github.theholywaffle.teamspeak3.api;

import java.util.OptionalInt;

/**
 * Restricts a channel group membership listing. Unset criteria match everything.
 */
public final class ChannelGroupFilter {

	private static final ChannelGroupFilter ALL = new ChannelGroupFilter(0, 0, 0);

	private final int channelId;
	private final int clientDatabaseId;
	private final int groupId;

	private ChannelGroupFilter(int channelId, int clientDatabaseId, int groupId) {
		this.channelId = channelId;
		this.clientDatabaseId = clientDatabaseId;
		this.groupId = groupId;
	}

	/** @return a filter matching every membership */
	public static ChannelGroupFilter all() { return ALL; }

	public ChannelGroupFilter inChannel(int id) { return new ChannelGroupFilter(positive(id), clientDatabaseId, groupId); }

	public ChannelGroupFilter ofClient(int databaseId) { return new ChannelGroupFilter(channelId, positive(databaseId), groupId); }

	public ChannelGroupFilter inGroup(int id) { return new ChannelGroupFilter(channelId, clientDatabaseId, positive(id)); }

	public OptionalInt channelId() { return channelId > 0 ? OptionalInt.of(channelId) : OptionalInt.empty(); }

	public OptionalInt clientDatabaseId() { return clientDatabaseId > 0 ? OptionalInt.of(clientDatabaseId) : OptionalInt.empty(); }

	public OptionalInt groupId() { return groupId > 0 ? OptionalInt.of(groupId) : OptionalInt.empty(); }

	private static int positive(int id) {
		if (id <= 0) throw new IllegalArgumentException("ID must be positive");
		return id;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null || other.getClass() != getClass()) return false;
		ChannelGroupFilter filter = (ChannelGroupFilter) other;
		return channelId == filter.channelId && clientDatabaseId == filter.clientDatabaseId && groupId == filter.groupId;
	}

	@Override
	public int hashCode() { return java.util.Objects.hash(channelId, clientDatabaseId, groupId); }

	@Override
	public String toString() {
		return "ChannelGroupFilter[channelId=" + channelId() + ", clientDatabaseId=" + clientDatabaseId() + ", groupId=" + groupId() + ']';
	}
}
