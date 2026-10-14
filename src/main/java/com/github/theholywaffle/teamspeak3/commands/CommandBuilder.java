package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.api.Property;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class CommandBuilder {

    private final String commandName;
    private final List<Parameter> parameters = new ArrayList<>();

    CommandBuilder(String commandName) {
        this.commandName = commandName;
    }

    CommandBuilder add(Parameter parameter) {
        parameters.add(parameter);
        return this;
    }

    CommandBuilder addIf(boolean test, Parameter parameter) {
        return test ? add(parameter) : this;
    }

    CommandBuilder addProperties(Map<? extends Property, String> properties) {
        if (properties == null) return this;
        properties.forEach((property, value) -> {
            if (property == null) throw new IllegalArgumentException("Property cannot be null");
            if (!property.isChangeable()) {
                throw new IllegalArgumentException(
                        property.getClass().getSimpleName() + " " + property.getName() + " is not changeable");
            }
            add(new KeyValueParam(property.getName(), value));
        });
        return this;
    }

    Command build() {
        return new Command(commandName, parameters);
    }
}
