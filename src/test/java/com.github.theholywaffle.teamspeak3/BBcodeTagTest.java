package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.bbcode.BBcodeBoldTag;
import com.github.theholywaffle.teamspeak3.bbcode.BBcodeItalicTag;
import com.github.theholywaffle.teamspeak3.bbcode.BBcodeTag;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author anthony
 * @since 18/06/17
 */
public class BBcodeTagTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void emptyParameter()
    {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("stringToWrap cannot be empty.");
        final BBcodeTag tag = new BBcodeBoldTag();
        final String textMessageContent = tag.wrap(""); // must be crashed
    }

    @Test
    public void nullParameter()
    {
        expectedException.expect(NullPointerException.class);
        expectedException.expectMessage("stringToWrap cannot be null.");
        final BBcodeTag tag = new BBcodeItalicTag();
        final String textMessageContent = tag.wrap(null); // must be crashed
    }
}
