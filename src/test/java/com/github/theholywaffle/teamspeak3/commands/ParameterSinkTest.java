package com.github.theholywaffle.teamspeak3.commands;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.commands.parameter.ArrayParameter;
import com.github.theholywaffle.teamspeak3.commands.parameter.KeyValueParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.OptionParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.ParameterSink;
import com.github.theholywaffle.teamspeak3.commands.parameter.RawParam;
import com.github.theholywaffle.teamspeak3.commands.parameter.ValueParam;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParameterSinkTest {

    /** Records puts as {@code key=value} and entry boundaries as {@code |}. */
    private static final class Recorder implements ParameterSink {
        final List<String> events = new ArrayList<>();

        @Override
        public void put(String key, String value) {
            events.add(key + "=" + value);
        }

        @Override
        public void nextEntry() {
            events.add("|");
        }
    }

    @Test
    void everyParameterKindPassesUnencodedValues() {
        var sink = new Recorder();
        var command = new CommandBuilder("x")
                .add(new KeyValueParam("name", "a b|c\\d"))
                .add(new OptionParam("topic"))
                .add(new ValueParam("positional"))
                .add(ArrayParameter.of("clid", List.of("1", "2")))
                .add(new RawParam("raw=a\\sb flag|raw=c"))
                .build();
        command.getParameters().forEach(parameter -> parameter.addTo(sink));
        assertEquals(
                List.of(
                        "name=a b|c\\d",
                        "-topic=",
                        "positional=",
                        "clid=1",
                        "|",
                        "clid=2",
                        "raw=a b",
                        "flag=",
                        "|",
                        "raw=c"),
                sink.events);
    }
}
