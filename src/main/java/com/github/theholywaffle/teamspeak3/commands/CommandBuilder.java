package com.github.theholywaffle.teamspeak3.commands;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2017 Bert De Geyter, Roger Baumgartner
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

import com.github.theholywaffle.teamspeak3.api.Property;
import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

final class CommandBuilder {

	private final String commandName;
	private final Collection<Parameter> parameters;

	public CommandBuilder(String commandName) {
		this(commandName, 0);
	}

	public CommandBuilder(String commandName, int expectedParamCount) {
		this.commandName = commandName;
		this.parameters = new ArrayList<>(expectedParamCount);
	}

	public CommandBuilder add(Parameter parameter) {
		parameters.add(parameter);
		return this;
	}

	public CommandBuilder addIf(boolean test, Parameter parameter) {
		if (test) {
			parameters.add(parameter);
		}
		return this;
	}

	public CommandBuilder addProperties(Map<? extends Property, String> properties) {
		if (properties == null || properties.isEmpty()) return this;

		ArrayParameter parameter = new ArrayParameter(1, properties.size());
		for (Map.Entry<? extends Property, String> entry : properties.entrySet()) {
			Property property = entry.getKey();
			String value = entry.getValue();

			if (property == null) {
				throw new IllegalArgumentException("Property cannot be null");
			} else if (!property.isChangeable()) {
				String className = property.getClass().getSimpleName();
				throw new IllegalArgumentException(className + " " + property.getName() + " is not changeable");
			}

			parameter.add(new KeyValueParam(property.getName(), value));
		}
		parameters.add(parameter);

		return this;
	}

	public Command build() {
		return new Command(commandName, parameters);
	}
}
