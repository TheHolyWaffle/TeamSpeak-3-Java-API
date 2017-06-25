package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.bbcode.BBcodeBoldTag;
import com.github.theholywaffle.teamspeak3.bbcode.BBcodeTag;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author anthony
 * @since 25/06/17
 */
public class BBcodeBoldTagTest {

    @Test
    public void constructorTest1()
    {
        final BBcodeTag boldTag = new BBcodeBoldTag();
    }
    @Test
    public void wrapMethodTest()
    {
        final BBcodeTag boldTag = new BBcodeBoldTag();
        final String textMessageContent = boldTag.wrap("My text message is bold!");
        assertThat(textMessageContent, equalTo("[b]My text message is bold![/b]"));
    }
}
