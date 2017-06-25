package com.github.theholywaffle.teamspeak3.bbcode;

/**
 * @author anthony
 * @version 0.1.0
 * @since 17/06/17
 */
public class BBcodeColorTag extends BBcodeTag {
    public BBcodeColorTag(final int rgbModel) {
        super("[color]", "[/color]", String.valueOf(rgbModel));
    }

    public BBcodeColorTag(final Color red) {
        super("[color]", "[/color]", red.toString());
    }
}
