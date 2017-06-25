package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.bbcode.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author anthony
 * @since 16/06/17
 */
public class BBcodeFormatterTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void constructorTest1()
    {
        final BBcodeFormatter formatter = new BBcodeFormatter("Simple buffered string. No bbcode here.");
    }
    @Test
    public void constructorTest2()
    {
        final BBcodeFormatter formatter =
                new BBcodeFormatter("Stylish buffered bbcode string.",
                        new BBcodeColorTag(Color.PURPLE),
                        new BBcodeItalicTag(),
                        new BBcodeBoldTag(),
                        new BBcodeUnderlineTag());
    }
    @Test
    public void textFormattingTest1()
    {
        final BBcodeFormatter formatter = new BBcodeFormatter("Simple buffered string. No bbcode here.");
        formatter.appendCarriageReturn()
                .append("red text message.", new BBcodeColorTag(Color.RED))
                .appendCarriageReturn()
                .append("bold green message.", new BBcodeBoldTag(), new BBcodeColorTag(Color.GREEN));
        final String textMessageContent = formatter.toString();
        assertThat(textMessageContent, equalTo("Simple buffered string. No bbcode here.\n" +
                "[color=RED]red text message.[/color]\n" +
                "[color=GREEN][b]bold green message.[/b][/color]"));
    }
    @Test
    public void textFormattingTest2()
    {
        final BBcodeFormatter formatter = new BBcodeFormatter()
                .append("raw text message")
                .append(".");
        final String textMessageContent = formatter.toBBcodeString();
        assertThat(textMessageContent, equalTo("raw text message."));
    }
    @Test
    public void textFormattingTest3()
    {
        final BBcodeFormatter formatter =
                new BBcodeFormatter("Stylish buffered bbcode string.",
                        new BBcodeUnderlineTag(),
                        new BBcodeBoldTag(),
                        new BBcodeItalicTag(),
                        new BBcodeColorTag(Color.PURPLE));
        final String textMessageContent = formatter.toBBcodeString();
        assertThat(textMessageContent, equalTo("[color=PURPLE][i][b][u]Stylish buffered bbcode string.[/u][/b][/i][/color]"));
    }
    @Test
    public void textFormattingTest4() throws MalformedURLException {
        final BBcodeFormatter formatter = new BBcodeFormatter("Click me!", new BBcodeURLTag(new URL("https://duckduckgo.com")));
        final String textMessageContent = formatter.toBBcodeString();
        assertThat(textMessageContent, equalTo("[url=https://duckduckgo.com]Click me![/url]"));
    }
    @Test
    public void stressTest1()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("The string cannot be null.\n" +
                "Please use the BBcodeFormatter() constructor instead.");
        final BBcodeFormatter formatter = new BBcodeFormatter(null, new BBcodeBoldTag());
    }
    @Test
    public void stressTest2()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("Your bbcode tags list cannot be null.\n" +
                "Please use the BBcodeFormatter(String) constructor instead.");
        final BBcodeFormatter formatter = new BBcodeFormatter("Hi there!", null);
    }
    @Test
    public void stressTest3()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("The string cannot be null.\n" +
                "Please use the BBcodeFormatter() constructor instead.");
        final BBcodeFormatter formatter = new BBcodeFormatter(null);
    }
    @Test
    public void stressTest4()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("string cannot be null.");
        final BBcodeFormatter formatter = new BBcodeFormatter("red text message", new BBcodeColorTag(Color.RED))
                .append(null);
    }
    @Test
    public void stressTest5()
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("string cannot be empty.");
        final BBcodeFormatter formatter = new BBcodeFormatter("red text message", new BBcodeColorTag(Color.RED))
                .append("");
    }
    @Test
    public void stressTest6()
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("string cannot be empty.");
        final BBcodeFormatter formatter = new BBcodeFormatter("red text message", new BBcodeColorTag(Color.RED))
                .append("", new BBcodeBoldTag());
    }
    @Test
    public void stressTest7()
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("string cannot be empty.");
        final BBcodeFormatter formatter = new BBcodeFormatter("red text message", new BBcodeColorTag(Color.RED))
                .append("", new BBcodeBoldTag(), new BBcodeItalicTag());
    }
}
