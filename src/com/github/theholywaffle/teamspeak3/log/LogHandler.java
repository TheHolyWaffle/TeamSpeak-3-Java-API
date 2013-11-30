package com.github.theholywaffle.teamspeak3.log;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {

	public void close() throws SecurityException {
	}

	public void flush() {
	}

	public void publish(LogRecord record) {
		if (record.getLevel().intValue() < Level.WARNING.intValue()) {
			System.out.println("[DEBUG] " + record.getMessage());
		} else {
			System.err.println("[DEBUG] [" + record.getLevel() + "] "
					+ record.getMessage());
		}
	}

}
