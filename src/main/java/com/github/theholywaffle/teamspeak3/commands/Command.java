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

import com.github.theholywaffle.teamspeak3.api.Callback;
import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Command {

	private static final int EMPTY_RESULT_SET_ERROR_ID = 1281;

	private final String command;
	private final Collection<Parameter> params = new LinkedList<>();

	private Callback callback;
	private DefaultArrayResponse response;
	private QueryError error;

	protected Command(String command) {
		this.command = command;
	}

	protected void add(Parameter p) {
		params.add(p);
	}

	public void feed(String str) {
		if (response == null) {
			response = new DefaultArrayResponse(str);
		} else {
			response.appendResponse(str);
		}
	}

	public void feedError(String err) {
		if (error != null) throw new IllegalStateException("Multiple errors for one command");

		final DefaultArrayResponse errorResponse = new DefaultArrayResponse(err);
		QueryError parsed = new QueryError(errorResponse.getFirstResponse().getMap());

		if (parsed.getId() == EMPTY_RESULT_SET_ERROR_ID) {
			if (response != null) throw new IllegalStateException("Got empty result set despite proper response");

			// Set empty response instead
			response = new DefaultArrayResponse();

			// Fake an ok-error
			Map<String, String> errorMap = new HashMap<>(2);
			errorMap.put("id", "0");
			errorMap.put("msg", "ok");
			error = new QueryError(errorMap);
		} else {
			error = parsed;
		}
	}

	public QueryError getError() {
		return error;
	}

	public String getName() {
		return command;
	}

	public Wrapper getFirstResponse() {
		return response.getFirstResponse();
	}

	public List<Wrapper> getResponse() {
		return response.getArray();
	}

	public String getRawResponse() {
		return response.getRawResponse();
	}

	public boolean isAnswered() {
		return (error != null);
	}

	public Callback getCallback() {
		return callback;
	}

	public void setCallback(Callback callback) {
		if (this.callback != null) throw new IllegalArgumentException("Callback already set");
		this.callback = callback;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(command);
		for (final Parameter p : params) {
			builder.append(' ').append(p);
		}
		return builder.toString();
	}
}
