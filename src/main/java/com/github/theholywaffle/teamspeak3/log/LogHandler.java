/*******************************************************************************
 * Copyright (c) 2013 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
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
