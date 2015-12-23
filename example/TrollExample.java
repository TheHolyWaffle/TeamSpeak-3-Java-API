/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
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

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.CommandFuture.SuccessListener;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A more advanced example of a query which trolls clients on a server
 * by moving them into other channels and sending them creepy messages.
 * <p>
 * Unlike the other examples, this class uses the asynchronous API
 * in combination with a {@code ScheduledExecutorService}.
 * </p>
 */
public class TrollExample {

	private static final Random RANDOM = new Random();

	// Troll-y messages because reason. They're not especially good, I'm sorry :(
	private static final String[] MESSAGES = {
			"Get off my server, damn kids!",
			"Tell me a joke!",
			"Hey Apple! Hey Apple! Apple! Hey Apple!",
			"A virus has been detected",
			"Move out of this channel or you will be banned in 15 seconds.",
			"Say something :3",
			"Boo!"
	};

	public static void main(String[] args) {
		new TrollExample();
	}

	private final TS3Query query;
	private final ScheduledExecutorService executor;
	private final HashMap<Integer, ClientTrollWorker> workers;

	public TrollExample() {
		// Start the query
		final TS3Config config = new TS3Config();
		config.setHost("77.77.77.77");
		config.setDebugLevel(Level.ALL);

		query = new TS3Query(config);
		query.connect();

		// And let's use one separate thread to troll everyone
		executor = new ScheduledThreadPoolExecutor(1);
		workers = new HashMap<>();

		// Log in, select the virtual server and set a nickname
		TS3Api api = query.getApi();
		api.login("serveradmin", "serveradminpassword");
		api.selectVirtualServerById(1);
		api.setNickname("Global Server Admin");

		api.registerEvent(TS3EventType.SERVER);
		api.addTS3Listeners(new TS3EventAdapter() {

			@Override
			public void onClientJoin(ClientJoinEvent e) {
				final int clientId = e.getClientId();
				final ClientTrollWorker worker = new ClientTrollWorker(clientId);
				workers.put(clientId, worker);

				// Let's wait a short moment until we begin trolling - 10 to 20 seconds
				long timeout = 10 + RANDOM.nextInt(11);
				executor.schedule(worker, timeout, TimeUnit.SECONDS);
			}

			@Override
			public void onClientLeave(ClientLeaveEvent e) {
				final ClientTrollWorker worker = workers.remove(e.getClientId());
				if (worker == null) return;
				worker.shutdown();
			}
		});
	}

	private class ClientTrollWorker implements Runnable {

		private final int clientId;
		private boolean cancelled;

		private ClientTrollWorker(int clientId) {
			this.clientId = clientId;
			cancelled = false;
		}

		@Override
		public void run() {
			// We were cancelled, abandon ship!
			if (cancelled) return;

			// Important!
			// We're using the asynchronous API so we don't block
			// our executor which has only 1 thread
			final TS3ApiAsync asyncApi = query.getAsyncApi();

			// Let's decide what to do
			int choice = RANDOM.nextInt(4); // 0 .. 3
			switch (choice) {
				case 0:
					// Poke the client with a random message
					String pokeMessage = MESSAGES[RANDOM.nextInt(MESSAGES.length)];
					asyncApi.pokeClient(clientId, pokeMessage);
					break;

				case 1:
					// Send the client a private message
					String privateMessage = MESSAGES[RANDOM.nextInt(MESSAGES.length)];
					asyncApi.sendPrivateMessage(clientId, privateMessage);
					break;

				case 2:
					// Kick the client to the default channel
					asyncApi.kickClientFromChannel(clientId);
					break;

				case 3:
					// Move client into a different channel
					// Fist, get all channels
					// (Using .getChannels every time works on small servers, but might
					//  not be a good idea on servers with 50+ clients!                )
					asyncApi.getChannels().onSuccess(new SuccessListener<List<Channel>>() {
						@Override
						public void handleSuccess(List<Channel> result) {
							// Once we retrieved the channels (some time in the future), choose one channel
							int randomChannelIndex = RANDOM.nextInt(result.size());
							Channel randomChannel = result.get(randomChannelIndex);

							// Then move the client into that channel
							asyncApi.moveClient(clientId, randomChannel.getId());
						}
					});
					break;
				default:
					// java.util.Random is broken
					throw new IllegalStateException("java.util.Random returned an illegal value.");
			}

			// And finally, re-schedule the runnable
			long timeout = RANDOM.nextInt(30) + 30; // Wait 30 to 60 seconds
			executor.schedule(this, timeout, TimeUnit.SECONDS);
		}

		private void shutdown() {
			cancelled = true;
		}
	}

}
