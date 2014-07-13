package com.github.theholywaffle.teamspeak3;

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

import java.io.IOException;

import com.github.theholywaffle.teamspeak3.commands.Command;

public class SocketReader extends Thread {

	private final TS3Query ts3;
	private volatile boolean stop;

	public SocketReader(TS3Query ts3) {
		super("SocketReader");
		this.ts3 = ts3;
		try {
			int i = 0;
			while (i < 4 || ts3.getIn().ready()) {
				TS3Query.log.info("< " + ts3.getIn().readLine());
				i++;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (ts3.getSocket() != null && ts3.getSocket().isConnected()
				&& ts3.getIn() != null && !stop) {
			try {
				if (ts3.getIn().ready()) {
					final String line = ts3.getIn().readLine();
					if (!line.isEmpty()) {
						final Command c = ts3.getCommandList().peek();
						if (line.startsWith("notify")) {
							TS3Query.log.info("< [event] " + line);
							new Thread(new Runnable() {

								public void run() {
									final String arr[] = line.split(" ", 2);
									ts3.getEventManager().fireEvent(arr[0],
											arr[1]);

								}
							}).start();

						} else if (c != null && c.isSent()) {
							TS3Query.log
									.info("[" + c.getName() + "] < " + line);
							if (line.startsWith("error")) {
								c.feedError(line.substring("error ".length()));
								if (c.getError().getId() != 0) {
									TS3Query.log.severe("[ERROR] "
											+ c.getError());
								}
								c.setAnswered();
								ts3.getCommandList().remove(c);
							} else if (!line.isEmpty()) {
								c.feed(line);
							}
						} else {
							TS3Query.log.info("< " + line);
						}
					}
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(50);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		TS3Query.log.warning("SocketReader has stopped!");
	}

	public void finish() {
		stop = true;
	}

}
