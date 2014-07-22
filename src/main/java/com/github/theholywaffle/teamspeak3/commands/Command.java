package com.github.theholywaffle.teamspeak3.commands;

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
import java.util.HashMap;
import java.util.List;

import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;

public class Command {

	private final String command;
	private boolean sent = false;
	private boolean answered = false;
	private DefaultArrayResponse response;
	private QueryError error;
	private final ArrayList<Parameter> params = new ArrayList<>();
	private String raw;

	public Command(String command) {
		this.command = command;
	}

	protected void add(Parameter p) {
		params.add(p);
	}

	public void feed(String str) {
		raw = str;
		if (response == null) {
			response = new DefaultArrayResponse(str);
		}
	}

	public void feedError(String err) {
		if (error == null) {
			error = new QueryError(new DefaultArrayResponse(err).getArray()
					.get(0));
		}
	}

	public QueryError getError() {
		return error;
	}

	public String getName() {
		return command;
	}

	public Wrapper getFirstResponse() {
		final List<HashMap<String, String>> resp = getResponse();
		if (resp.size() > 0) {
			return new Wrapper(resp.get(0));
		}
		return new Wrapper(new HashMap<String, String>());
	}

	public List<HashMap<String, String>> getResponse() {
		if (response == null) {
			return new DefaultArrayResponse("").getArray();
		}
		return response.getArray();
	}

	public boolean isAnswered() {
		return answered;
	}

	public boolean isSent() {
		return sent;
	}

	public void setAnswered() {
		answered = true;
	}

	public void setSent() {
		sent = true;
	}
	
	public String toString() {
		String str = command;
		for (final Parameter p : params) {
			str += " " + p;
		}
		return str;
	}

	public String getRaw() {
		return raw;
	}

}
