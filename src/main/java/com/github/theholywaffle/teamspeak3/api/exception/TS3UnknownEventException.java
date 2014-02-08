/*******************************************************************************
 * Copyright (c) 2014 Bert De Geyter (https://github.com/TheHolyWaffle).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bert De Geyter (https://github.com/TheHolyWaffle)
 ******************************************************************************/
package com.github.theholywaffle.teamspeak3.api.exception;

public class TS3UnknownEventException extends TS3Exception {

	private static final long serialVersionUID = -9179157153557357715L;

	public TS3UnknownEventException(String event) {
		super(event+" [Please report this exception to the developer!]");
	}

}
