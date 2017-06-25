package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.bbcode.BBcodeTag;
import com.github.theholywaffle.teamspeak3.bbcode.BBcodeUnderlineTag;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author anthony
 * @since 25/06/17
 */
public class BBcodeUnderlineTagTest {

    @Test
    public void constructorTest1()
    {
        final BBcodeTag underlineTag = new BBcodeUnderlineTag();
    }

    @Test
    public void wrapMethodTest()
    {
        final BBcodeTag underlineTag = new BBcodeUnderlineTag();
        final String textMessageContent = underlineTag.wrap("My underline text message!");
        assertThat(textMessageContent, equalTo("[u]My underline text message![/u]"));
    }
}
