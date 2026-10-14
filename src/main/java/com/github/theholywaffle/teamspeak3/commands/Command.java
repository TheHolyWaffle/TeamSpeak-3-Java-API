package com.github.theholywaffle.teamspeak3.commands;

import com.github.theholywaffle.teamspeak3.commands.parameter.Parameter;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class Command {

    private final String name;
    private final List<Parameter> parameters;
    private final Set<CommandTrait> traits;
    private final CompletableFuture<DefaultArrayResponse> future = new CompletableFuture<>();

    Command(String name, List<Parameter> parameters) {
        this.name = name;
        this.parameters = List.copyOf(parameters);
        this.traits = CommandTrait.of(name);
    }

    public String getName() {
        return name;
    }

    /** @return what this command depends on or changes beyond its own request */
    public Set<CommandTrait> getTraits() {
        return traits;
    }

    /** @return the unencoded parameters, in protocol order */
    public List<Parameter> getParameters() {
        return parameters;
    }

    public CompletableFuture<DefaultArrayResponse> getFuture() {
        return future;
    }

    /** @return the command line sent to the server, including secrets */
    public String encoded() {
        var line = new StringBuilder(name);
        for (Parameter parameter : parameters) parameter.appendTo(line.append(' '));
        return line.toString();
    }

    /** @return the command line with {@linkplain ProtocolRedactor secrets} redacted */
    @Override
    public String toString() {
        return ProtocolRedactor.redactLine(encoded());
    }
}
