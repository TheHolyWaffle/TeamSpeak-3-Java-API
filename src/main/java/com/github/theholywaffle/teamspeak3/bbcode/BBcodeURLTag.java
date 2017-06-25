package com.github.theholywaffle.teamspeak3.bbcode;

import java.net.URL;
import java.util.Objects;

/**
 * @author anthony
 * @since 24/06/17
 */
public class BBcodeURLTag extends BBcodeTag {
    /**
     * Wraps your text message with {@code [url][/url]} bbcode tags
     * and puts {@code urlString} as URL.<br>
     * For example:
     * <pre>{@code
     * final BBcodeTag url = new BBcodeURLTag("duckduckgo.com");
     * final String yourBBcodeTextMessage = url.wrap("Click here!");
     * System.out.println(yourBBcodeTextMessage); // prints [url=duckduckgo.com]Click here![/url]}</pre>
     * @param urlString The URL to submit as bbcode tag parameter..
     * @see #BBcodeURLTag(String)
     */
    public BBcodeURLTag(final String urlString) {
        super("[url]", "[/url]", urlSanitizing(urlString));
    }

    private static String urlSanitizing(final String urlString) {
        if(Objects.requireNonNull(urlString, "URL string reference cannot be null.")
                .isEmpty())
        {
            throw new IllegalArgumentException("URL string cannot be empty.");
        }
        return urlString;
    }

    /**
     * Wraps your text message with {@code [url][/url]} bbcode tags
     * and puts {@code urlString} as URL.
     * @param url The URL to submit as bbcode tag parameter.
     */
    public BBcodeURLTag(final URL url)
    {
        this(url.toString());
    }
}
