package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.bbcode.Color;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author anthony
 * @since 17/06/17
 */
public class ColorTest {

    @Test
    public void redColorRepresentation()
    {
        final Color red = Color.RED;
        assertThat(red.toString(), is(equalTo("RED")));
    }
    @Test
    public void greenColorRepresentation()
    {
        final Color green = Color.GREEN;
        assertThat(green.toString(), is(equalTo("GREEN")));
    }
    @Test
    public void blueColorRepresentation()
    {
        final Color blue = Color.BLUE;
        assertThat(blue.toString(), is(equalTo("BLUE")));
    }
    @Test
    public void yellowColorRepresentation()
    {
        final Color yellow = Color.YELLOW;
        assertThat(yellow.toString(), is(equalTo("YELLOW")));
    }
    @Test
    public void orangeColorRepresentation()
    {
        final Color orange = Color.ORANGE;
        assertThat(orange.toString(), is(equalTo("ORANGE")));
    }
    @Test
    public void greyColorRepresentation()
    {
        final Color grey = Color.GREY;
        assertThat(grey.toString(), is(equalTo("GREY")));
    }
    @Test
    public void purpleColorRepresentation()
    {
        final Color purple = Color.PURPLE;
        assertThat(purple.toString(), is(equalTo("PURPLE")));
    }
    @Test
    public void brownColorRepresentation()
    {
        final Color brown = Color.BROWN;
        assertThat(brown.toString(), is(equalTo("BROWN")));
    }
    @Test
    public void beigeColorRepresentation()
    {
        final Color beige = Color.BEIGE;
        assertThat(beige.toString(), is(equalTo("BEIGE")));
    }
    @Test
    public void cyanColorRepresentation()
    {
        final Color cyan = Color.CYAN;
        assertThat(cyan.toString(), is(equalTo("CYAN")));
    }
    @Test
    public void navyColorRepresentation()
    {
        final Color navy = Color.NAVY;
        assertThat(navy.toString(), is(equalTo("NAVY")));
    }
}
