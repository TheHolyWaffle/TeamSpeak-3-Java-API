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
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {

	private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); // ISO 8601
	private final boolean writeToFile;
	private final PrintWriter fileWriter;

	public LogHandler(boolean writeToFile) {
		boolean toFile = writeToFile;
		PrintWriter out = null;

		if (toFile) {
			try {
				File log = new File("teamspeak.log");
				if (!log.exists()) {
					log.createNewFile();
				}
				out = new PrintWriter(new BufferedWriter(new FileWriter(log, true)), true);
			} catch (IOException io) {
				System.err.println("Could not set log handler! Using System.out instead");
				io.printStackTrace();
				toFile = false;
			}
		}

		this.writeToFile = toFile;
		this.fileWriter = out;
	}

	@Override
	public void close() {
		if (writeToFile) {
			fileWriter.flush();
			fileWriter.close();
		}
	}

	@Override
	public void flush() {
		if (writeToFile) {
			fileWriter.flush();
		}
	}

	@Override
	public void publish(LogRecord record) {
		StringBuilder logMessage = new StringBuilder();

		// Start the log message with a timestamp
		logMessage.append("[").append(format.format(new Date())).append("] ");

		// Include the severity if we're dealing with a warning or an error
		if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
			logMessage.append("[").append(record.getLevel()).append("] ");
		}

		logMessage.append(record.getMessage());

		Throwable t = record.getThrown();
		if (t != null) {
			logMessage.append("\n");
			StringWriter stackTrace = new StringWriter();
			t.printStackTrace(new PrintWriter(stackTrace));
			logMessage.append(stackTrace.getBuffer());
		}

		// Write to System.out and, if enabled, to a log file
		System.out.println(logMessage);
		if (writeToFile) {
			fileWriter.println(logMessage);
		}
	}
}
