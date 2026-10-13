package com.github.theholywaffle.teamspeak3.commands;

import java.util.Locale;
import java.util.Set;

/**
 * Removes passwords, tokens and file transfer keys from ServerQuery text before it is logged or
 * shown in diagnostics.
 */
public final class ProtocolRedactor {

    /** Replaces each redacted value. */
    public static final String REDACTED = "<redacted>";

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
     * <p>Values of {@linkplain #isSecret(String) secret} properties are replaced in every row, and
     * so is the positional password of {@code login}. Command names, other properties and the line
     * structure are kept.
     *
     * @param line encoded ServerQuery text
     * @return the line with secret values replaced by {@value #REDACTED}
     */
    public static String redactLine(String line) {
        var redacted = new StringBuilder(line.length());
        boolean login = false;
        int positional = 0;
        int start = 0;
        for (int i = 0; i <= line.length(); i++) {
            char separator = i < line.length() ? line.charAt(i) : '\n';
            if (separator != ' ' && separator != '|' && i < line.length()) continue;
            String token = line.substring(start, i);
            int equals = token.indexOf('=');
            if (start == 0 && equals < 0) {
                login = token.equals("login");
                redacted.append(token);
            } else if (equals > 0 && equals < token.length() - 1 && isSecret(token.substring(0, equals))) {
                redacted.append(token, 0, equals + 1).append(REDACTED);
            } else if (login && equals < 0 && !token.isEmpty() && !token.startsWith("-") && ++positional > 1) {
                redacted.append(REDACTED);
            } else {
                redacted.append(token);
            }
            if (i < line.length()) redacted.append(separator);
            start = i + 1;
        }
        return redacted.toString();
    }
}
