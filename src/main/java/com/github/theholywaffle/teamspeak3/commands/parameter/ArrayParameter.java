package com.github.theholywaffle.teamspeak3.commands.parameter;

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
import java.util.List;

public class ArrayParameter extends Parameter {

	private final int parametersPerEntry;
	private final List<Parameter> parameters;

	public ArrayParameter(int numberOfEntries) {
		this(numberOfEntries, 1);
	}

	public ArrayParameter(int numberOfEntries, int parametersPerEntry) {
		this.parametersPerEntry = parametersPerEntry;
		this.parameters = new ArrayList<>(numberOfEntries * parametersPerEntry);
	}

	public ArrayParameter add(Parameter p) {
		parameters.add(p);
		return this;
	}

	@Override
	public String build() {
		if (parameters.isEmpty()) return "";

		StringBuilder str = new StringBuilder();

		// First entry without |
		str.append(parameters.get(0).build());
		for (int i = 1; i < parametersPerEntry; ++i) {
			str.append(' ').append(parameters.get(i).build());
		}

		// Remaining entries separated with |
		for (int offset = parametersPerEntry; offset < parameters.size(); offset += parametersPerEntry) {
			str.append('|').append(parameters.get(offset).build());
			for (int i = 1; i < parametersPerEntry; ++i) {
				str.append(' ').append(parameters.get(offset + i).build());
			}
		}

		return str.toString();
	}
}
