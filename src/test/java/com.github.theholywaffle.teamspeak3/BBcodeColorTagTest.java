package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.bbcode.BBcodeColorTag;
import com.github.theholywaffle.teamspeak3.bbcode.BBcodeTag;
import com.github.theholywaffle.teamspeak3.bbcode.Color;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author anthony
 * @since 17/06/17
 */
public class BBcodeColorTagTest {

    @Test
    public void constructorTest1()
    {
        final BBcodeTag colorTag = new BBcodeColorTag(0xFF0000);
    }

    @Test
    public void constructorTest2()
    {
        final BBcodeTag colorTag = new BBcodeColorTag(Color.RED);
    }

    @Test
    public void wrapMethodTest1()
    {
        final BBcodeTag colorTag = new BBcodeColorTag(Color.BEIGE);
        final String s = colorTag.wrap("My awesome text message!");
        assertThat(s, is(equalTo("[color=BEIGE]My awesome text message![/color]")));
    }

    @Test
    public void wrapMethodTest2()
    {
        final BBcodeColorTag colorTag = new BBcodeColorTag(Color.BLUE);
        final String s = colorTag.wrap("My awesome text message!");
        assertThat(s, equalTo("[color=BLUE]My awesome text message![/color]"));
    }
}
