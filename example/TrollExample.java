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

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;

public class TrollExample {

	public static void main(String[] args) {
		final TS3Config config = new TS3Config();
		config.setHost("77.77.77.77");
		config.setDebugLevel(Level.ALL);
		config.setLoginCredentials("serveradmin", "serveradminpassword");

		final TS3Query query = new TS3Query(config);
		query.connect();

		final TS3Api api = query.getApi();
		api.selectVirtualServerById(1);
		api.setNickname("PutPutBot");
		api.sendChannelMessage("PutPutBot is online!");

		api.registerAllEvents();
	}

	public static void troll(String name, TS3Api bot) {
		final ArrayList<Channel> channels = new ArrayList<>();
		for (final Channel c : bot.getChannels()) {
			if (c.getTotalClients() > 0) {
				channels.add(c);
			}
		}

		final int clientId = bot.getClientByName(name).get(0).getId();

		final Random r = new Random();
		while (true) {
			final int channelId = channels.get(r.nextInt(channels.size()))
					.getId();
			bot.moveClient(clientId, channelId);
			try {
				Thread.sleep(2000);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void troll2(String name, TS3Api bot) {
		final int clientId = bot.getClientByName(name).get(0).getId();

		int i = 0;
		final Random r = new Random();
		while (true) {
			try {
				Thread.sleep(r.nextInt(30_000) + 30_000);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
			bot.pokeClient(clientId, i + "");
			i++;
		}
	}

}
