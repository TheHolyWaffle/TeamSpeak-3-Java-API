package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;

import java.util.Map;

/** Compiled example used by the launcher against its fresh server. */
final class DevelopmentServerExample {
	private DevelopmentServerExample() {}

	static void run(TS3Config config) {
		TS3Query query = new TS3Query(config);
		try {
			query.connect();
			TS3Api api = query.getApi();
			api.selectVirtualServerById(1);
			int channel = api.channels().create("Development example | literal \\s",
					Map.of(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1"));
			try {
				if (api.channels().list().stream().noneMatch(c -> c.getId() == channel)) {
					throw new IllegalStateException("Example channel was not listed by the server");
				}
				System.out.println("Compiled example succeeded: version=" + api.instance().version().getVersion()
						+ ", virtualServer=" + api.whoAmI().getVirtualServerId() + ", channel=" + channel);
			} finally { api.channels().delete(channel, true); }
		} finally { query.exit(); }
	}
}
