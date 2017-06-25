package com.github.theholywaffle.teamspeak3.bbcode;

import java.util.Objects;

/**
 * Formats your string with compatible TeamSpeak bbcode tags.
 * @author anthony
 * @since 16/06/17
 */
public class BBcodeFormatter {

    private final StringBuilder builder;

    /**
     * Initializes the formatter, appends a string and wraps it.
     * <p>{@code bbcodeTags} as in will be used to format your string.</p>
     * @param s Your string.
     * @param bbcodeTags BBcode tags with which you want to format your string.
     * @exception NullPointerException If your string is null.
     * @exception NullPointerException If your bbcode tags list is null.
     * @exception IllegalArgumentException If your string is empty.
     * @exception IllegalArgumentException If your bbcode tags list is empty.
     */
    public BBcodeFormatter(String s, BBcodeTag... bbcodeTags) {
        Objects.requireNonNull(s, "The string cannot be null.\n" +
                "Please use the BBcodeFormatter() constructor instead.");
        Objects.requireNonNull(bbcodeTags, "Your bbcode tags list cannot be null.\n" +
                "Please use the BBcodeFormatter(String) constructor instead.");
        if(s.isEmpty())
            throw new IllegalArgumentException("The string cannot be empty.\n" +
                    "Please use the BBcodeFormatter() constructor instead.");
        if(bbcodeTags.length == 0)
            throw new IllegalArgumentException("Your bbcode tags list cannot be empty.\n" +
                    "Please use the BBcodeFormatter(String) constructor instead.");
        this.builder = new StringBuilder();
        final String content = buildBBcodeString(s, bbcodeTags);
        builder.append(content);
    }

    /**
     * Initializes the formatter and appends a string.
     * @param s Your string.
     */
    public BBcodeFormatter(final String s) {
        Objects.requireNonNull(s, "The string cannot be null.\n" +
                "Please use the BBcodeFormatter() constructor instead.");
        if(s.isEmpty())
            throw new IllegalArgumentException("The string cannot be empty.\n" +
                    "Please use the BBcodeFormatter() constructor instead.");
        this.builder = new StringBuilder(s);
    }

    /**
     * Initializes the formatter.
     */
    public BBcodeFormatter() {
        this.builder = new StringBuilder();
    }

    /**
     * Puts a carriage return in your string.
     * @return Formatter instance.
     */
    public BBcodeFormatter appendCarriageReturn() {
        builder.append("\n");
        return this;
    }

    /**
     * Puts your string into buffer and formats it.
     * @param s Your string.
     * @param bbcodeTags Your bbcode tags list.
     */
    public BBcodeFormatter append(String s, BBcodeTag... bbcodeTags) {
        Objects.requireNonNull(s, "string cannot be null.");
        if(s.isEmpty()) throw new IllegalArgumentException("string cannot be empty.");
        Objects.requireNonNull(bbcodeTags, "bbcode tags list cannot be null.");
        if(bbcodeTags.length == 0) throw new IllegalArgumentException("bbcode tags list cannot be empty.");
        builder.append(buildBBcodeString(s, bbcodeTags));
        return this;
    }

    /**
     * Puts your string into buffer and formats it.
     * @param s Your string.
     * @param bbcodeTag Your bbcode tag.
     * @return Formatter instance.
     */
    public BBcodeFormatter append(String s, BBcodeTag bbcodeTag) {
        Objects.requireNonNull(s, "string cannot be null.");
        if(s.isEmpty()) throw new IllegalArgumentException("string cannot be empty.");
        Objects.requireNonNull(bbcodeTag, "bbcode tag cannot be null.");
        builder.append(bbcodeTag.wrap(s));
        return this;
    }

    /**
     * Puts your string into buffer, no style applied.
     * @param s Your string.
     * @return Formatter instance.
     */
    public BBcodeFormatter append(String s) {
        Objects.requireNonNull(s, "string cannot be null.");
        if(s.isEmpty()) throw new IllegalArgumentException("string cannot be empty.");
        builder.append(s);
        return this;
    }

    /**
     * May be used to build bbcode string with several bbcode tags.
     * @param s Your string.
     * @param bbcodeTags Your bbcode tags list.
     * @return Your built-in bbcode string.
     */
    private String buildBBcodeString(String s, BBcodeTag[] bbcodeTags) {
        Objects.requireNonNull(s, "string cannot be null.");
        if(s.isEmpty()) throw new IllegalArgumentException("string cannot be empty.");
        Objects.requireNonNull(bbcodeTags, "bbcode tags list cannot be null.");
        if(bbcodeTags.length == 0) throw new IllegalArgumentException("bbcode tags list cannot be empty.");
        String content = null;
        for(int accumulator = 0; accumulator < bbcodeTags.length; accumulator += 1)
        {
            if(accumulator == 0)
            {
                content = bbcodeTags[0].wrap(s);
                continue;
            }
            final String lastContentVersion = content;
            content = bbcodeTags[accumulator].wrap(lastContentVersion);
        }
        return content;
    }

    /**
     * @return Your text message wrapped, formatted.
     */
    public String toBBcodeString() {
        return builder.toString();
    }

    @Override
    public String toString()
    {
        return builder.toString();
    }
}
