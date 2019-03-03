package com.github.theholywaffle.teamspeak3;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2018 Bert De Geyter, Roger Baumgartner
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

import com.github.theholywaffle.teamspeak3.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

class StreamWriter extends Thread {

	private static final Logger log = LoggerFactory.getLogger(StreamWriter.class);

	private final Connection con;
	private final PrintWriter out;
	private final int floodRate;
	private final boolean logComms;

	StreamWriter(Connection connection, OutputStream outStream, TS3Config config) {
		super("[TeamSpeak-3-Java-API] StreamWriter");

		con = connection;
		out = new PrintWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8), true);
		floodRate = config.getFloodRate().getMs();
		logComms = config.getEnableCommunicationsLogging();
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				if (floodRate > 0) Thread.sleep(floodRate);

				Command command = con.getCommandQueue().transferCommand();
				if (command == null) continue;

				con.resetIdleTime();
				String message = command.toString();

				if (logComms) log.debug("[{}] > {}", command.getName(), message);
				out.println(message);
			}
		} catch (InterruptedException e) {
			interrupt(); // Regular shutdown
		}

		out.close();

		if (!isInterrupted()) {
			log.warn("StreamWriter has stopped!");
			con.internalDisconnect();
		}
	}
}
