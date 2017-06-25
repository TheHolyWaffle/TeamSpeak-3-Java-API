package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.bbcode.BBcodeItalicTag;
import com.github.theholywaffle.teamspeak3.bbcode.BBcodeTag;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author anthony
 * @since 25/06/17
 */
public class BBcodeItalicTagTest {

    @Test
    public void constructorTest1()
    {
        final BBcodeTag italicTag = new BBcodeItalicTag();
    }
    @Test
    public void wrapMethodTest()
    {
        final BBcodeTag italicTag = new BBcodeItalicTag();
        final String textMessageContent = italicTag.wrap("My italic text message!");
        assertThat(textMessageContent, equalTo("[i]My italic text message![/i]"));
    }
}
