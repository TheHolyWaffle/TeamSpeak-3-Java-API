package com.github.theholywaffle.teamspeak3.bbcolor;

import java.util.HashMap;
import java.util.Map;

public enum BBColor {

    BLUE("[color=blue]"),
    RED("[color=red]"),
    GREEN("[color=green]"),
    YELLOW("[color=yellow]"),
    PINK("[color=pink]"),
    ORANGE("[color=orange]"),
    PURPLE("[color=purple]"),
    BROWN("[color=brown]"),
    BLACK("[color=black]"),
    GREY("[color=grey]"),
    WHITE("[color=white]"),
    CRIMSON("[color=Crimson]"),
    CYAN("[color=Cyan]"),
    ANTIQUE_WHITE("[color=Antiquewhite]"),
    AQUAMARINE("[color=Aquamarine]"),
    BLUE_PURPLE("[color=purple]"),
    BURLY_WOOD("[color=Burlywood]"),
    CADETBLUE("[color=Cadetblue]"),
    CHARTREUSE("[color=Chartreuse]"),
    CHOCOLATE("[color=Chocolate]"),
    CORAL("[color=Coral]"),
    SALMON("[color=Salmon]"),
    CORN_FLOWER_BLUE("Cornflowerblue"),
    CORN_SILK("[color=Cornsilk]"),
    GOLD("[color=gold]"),
    LAWN_GREEN("[color=Lawngreen]"),
    LIME("[color=lime]"),
    GREEN_YELLOW("[color=darkgreen]"),
    HOT_PINK("[color=Hotpink]"),
    INDIGO("[color=Indigo]"),
    LITE_GREY("[color=Darkgray]"),
    DARK_BLUE("[color=Darkblue]"),
    DARK_CYAN("[color=Darkcyan"),
    DARK_GOLDEN("[color=Darkgoldenrod]"),
    DARK_GREEN("[color=Darkgreen]"),
    DARK_KHAKI("[color=Darkkhaki]"),
    DARK_ORANGE("[color=Darkorange]"),
    DARK_RED("[color=Darkred]"),
    DARK_SALMON("[color=Darksalmon]"),
    DARK_PINK("[color=Deeppink]"),
    BOLD("[b]"),
    ITALIC("[i]"),
    UNDERLINE("[u]"),
    RESET_BOLD("[/b]"),
    RESET_ITALIC("[/i]"),
    RESET_UNDERLINE("[/u]");

    private static final Map<String, BBColor> BY_COLOR = new HashMap<>();

    static {
        for (BBColor bbColor : values()) {
            BY_COLOR.put(bbColor.color, bbColor);
        }
    }

    public final String color;

    BBColor(String color) {
        this.color = color;
    }

    public static BBColor valueOfColor(String color) {
        return BY_COLOR.get(color);
    }

    @Override
    public String toString() {
        return this.color;
    }
}
