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
package com.github.theholywaffle.teamspeak3.api.exception;

public class TS3Exception extends RuntimeException {

	private static final long serialVersionUID = 7167169981592989359L;

	public TS3Exception(String msg) {
		super(msg + " [Please report this exception to the developer!]");
	}

    public TS3Exception(String msg, Throwable cause) {
        super(msg, cause);
    }
}
