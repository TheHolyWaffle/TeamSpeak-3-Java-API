/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2015 Bert De Geyter
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
import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import com.github.theholywaffle.teamspeak3.api.CommandFuture;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectingConnectionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

/**
 * An example demonstrating the reconnect feature, based on the already present ChatBotExample.
 */
public class ReconnectExample {

	// Since this ID changes when we reconnect, we need to keep track of it!
	private static int clientId;

	public static void main(String[] args) {
		final TS3Config config = new TS3Config();
		config.setHost("77.77.77.77");
		config.setDebugLevel(Level.ALL);

		// Make stuff run every time the query (re)connects
		config.setConnectionHandler(new ReconnectingConnectionHandler() {
			@Override
			public void setUpQuery(TS3Query ts3Query) {
				stuffThatNeedsToRunEveryTimeTheQueryConnects(ts3Query.getApi());
			}
		});

		final TS3Query query = new TS3Query(config);
		// Here "stuffThatNeedsToRunEveryTimeTheQueryConnects" will be run!
		// (And every time the query reconnects)
		query.connect();

		// Then do other stuff that only needs to be done once
		stuffThatOnlyEverNeedsToBeRunOnce(query.getApi());

		doSomethingThatTakesAReallyLongTime(query.getAsyncApi());
	}

	private static void stuffThatNeedsToRunEveryTimeTheQueryConnects(TS3Api api) {
		// Logging in, selecting the virtual server, selecting a channel
		// and setting a nickname needs to be done every time we reconnect
		api.login("serveradmin", "serveradminpassword");
		api.selectVirtualServerById(1);
		api.setNickname("PutPutBot");
		// api.moveQuery(x);

		// What events we listen to also resets
		api.registerEvent(TS3EventType.TEXT_CHANNEL, 0);

		// Out clientID changes every time we connect and we need it
		// for our event listener, so we need to store the ID in a field
		clientId = api.whoAmI().getId();
	}

	private static void stuffThatOnlyEverNeedsToBeRunOnce(final TS3Api api) {
		// We only want to greet people once
		api.sendChannelMessage("PutPutBot is online!");

		// On the API side of things, you only need to register your TS3Listeners once!
		// These are not affected when the query disconnects.
		api.addTS3Listeners(new TS3EventAdapter() {

			@Override
			public void onTextMessage(TextMessageEvent e) {
				// Only react to channel messages not sent by the query itself
				if (e.getTargetMode() == TextMessageTargetMode.CHANNEL && e.getInvokerId() != clientId) {
					// Greet whoever sent a message in the default channel
					// (Yes this isn't smart or useful, this is just an example)
					// Message: "Hello <client name>!"
					api.sendChannelMessage("Hello " + e.getInvokerName() + "!");
				}
			}
		});
	}

	private static void doSomethingThatTakesAReallyLongTime(TS3ApiAsync api) {
		// If you've modified the code of this example to run it on a local server, you
		// should try stopping and then restarting your TS3 server when this code executes

		final Collection<Integer> createdChannelIds = new ArrayList<>("Hello World".length());
		final Map<ChannelProperty, String> channelOptions = Collections.singletonMap(ChannelProperty.CHANNEL_FLAG_PERMANENT, "1");

		// Queue up a lot of commands
		int counter = 1;
		for (char c : "Hello World".toCharArray()) {
			// <Number> - <Letter>
			String name = String.valueOf(counter++) + " - " + c;

			// Create a permanent channel with that name
			CommandFuture<Integer> create = api.createChannel(name, channelOptions);
			// and store its ID (once it's created) in a list so we can delete it later
			create.onSuccess(new CommandFuture.SuccessListener<Integer>() {
				@Override
				public void handleSuccess(Integer result) {
					createdChannelIds.add(result);
				}
			});

			// Artificial delay
			api.whoAmI();
		}

		// If an disconnect happens while these commands are being sent, you'll notice that
		// - the query automatically reconnects
		// - the query is logged in, chooses the correct virtual server, etc.
		// - the execution resumes at the point it left off when the query was disconnected
		// - no channels are missing -> no commands were left out
		// - all channels will be removed again -> no callbacks were left out

		// Wait 5 seconds after everything's done, then undo the mess we've created
		api.whoAmI().getUninterruptibly(); // Wait for all previous commands to complete
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		for (Integer channelId : createdChannelIds) {
			api.deleteChannel(channelId, true);
		}
	}
}
