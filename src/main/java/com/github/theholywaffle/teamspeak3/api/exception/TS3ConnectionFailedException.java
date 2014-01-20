/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle) - initial API and implementation
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3.api.exception;

public class TS3ConnectionFailedException extends TS3Exception{

	private static final long serialVersionUID = 6849777544299282019L;

	public TS3ConnectionFailedException(Throwable c) {
		super("An error occurred while sending a command to a teamspeak server", c);
	}

}
