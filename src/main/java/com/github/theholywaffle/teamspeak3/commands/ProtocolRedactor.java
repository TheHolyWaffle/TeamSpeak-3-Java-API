package com.github.theholywaffle.teamspeak3.commands;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes passwords, tokens and file transfer keys from ServerQuery text before it is logged or
 * shown in diagnostics.
 */
public final class ProtocolRedactor {

    /** Replaces each redacted value. */
    public static final String REDACTED = "<redacted>";

    /** Separated by spaces between parameters and by pipes between rows. */
    private static final Pattern TOKEN = Pattern.compile("[^ |]+");

    private static final Set<String> SECRET_KEYS = Set.of("cpw", "tcpw", "pw", "pw_clear", "token", "ftkey", "apikey");

    private ProtocolRedactor() {}

    /**
     * @param key a ServerQuery property name, such as {@code client_login_password}
     * @return whether values of this property are secrets: passwords, tokens, API keys and file
     *     transfer keys
     */
    public static boolean isSecret(String key) {
        String name = key.toLowerCase(Locale.ROOT);
        return (name.endsWith("password") && !name.endsWith("flag_password")) || SECRET_KEYS.contains(name);
    }

    /**
     * Redacts one encoded command, response, notification or error line.
     *
     * <p>Values of {@linkplain #isSecret(String) secret} properties are replaced in every row. In a
     * {@code login} command, every argument after the user name is replaced: a positional password
     * may contain {@code =} or start with {@code -}. Command names, other properties and the line
     * structure are kept.
     *
     * @param line encoded ServerQuery text
     * @return the line with secret values replaced by {@value #REDACTED}
     */
    public static String redactLine(String line) {
        boolean login = line.equals("login") || line.startsWith("login ");
        Matcher tokens = TOKEN.matcher(line);
        var redacted = new StringBuilder(line.length());
        for (int index = 0; tokens.find(); index++) {
            String token = login && index > 1 ? REDACTED : redactProperty(tokens.group());
            tokens.appendReplacement(redacted, Matcher.quoteReplacement(token));
        }
        return tokens.appendTail(redacted).toString();
    }

    private static String redactProperty(String token) {
        int equals = token.indexOf('=');
        return equals > 0 && equals < token.length() - 1 && isSecret(token.substring(0, equals))
                ? token.substring(0, equals + 1) + REDACTED
                : token;
    }
}
