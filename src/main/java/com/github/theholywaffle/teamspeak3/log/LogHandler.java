package com.github.theholywaffle.teamspeak3.log;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {

	private static final DateFormat format = DateFormat.getInstance();
	private final boolean debugToFile;
	private File log;

	public LogHandler(boolean debugToFile) {
		this.debugToFile = debugToFile;
		if (this.debugToFile) {
			log = new File("teamspeak.log");
			if (!log.exists()) {
				try {
					log.createNewFile();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void close() throws SecurityException {
	}

	@Override
	public void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		if (record.getLevel().intValue() < Level.WARNING.intValue()) {
			System.out.println("[DEBUG] " + record.getMessage());
		} else {
			System.err.println("[DEBUG] [" + record.getLevel() + "] "
					+ record.getMessage());
		}
		if (debugToFile) {
			try (PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(log, true)))) {
				out.println("[" + format.format(new Date()) + "]["
						+ record.getLevel() + "] " + record.getMessage());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
}
