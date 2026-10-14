package com.github.theholywaffle.teamspeak3;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.net.ssl.SSLContext;

/**
 * Immutable settings of a {@link TS3WebQuery}. Create one with {@link #builder(URI, String)}.
 * <p>
 * The API key is sent only in the {@code x-api-key} header, never in a URL, and is excluded from
 * {@link #toString()}. HTTPS is required unless plaintext HTTP is explicitly allowed.
 * </p>
 */
public final class WebQueryConfig {

    private final URI baseUri;
    private final String apiKey;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final Duration closeTimeout;
    private final int requestCapacity;
    private final SSLContext sslContext;
    private final boolean plaintextHttpAllowed;

    private WebQueryConfig(Builder builder) {
        baseUri = builder.baseUri;
        apiKey = builder.apiKey;
        connectTimeout = builder.connectTimeout;
        requestTimeout = builder.requestTimeout;
        closeTimeout = builder.closeTimeout;
        requestCapacity = builder.requestCapacity;
        sslContext = builder.sslContext;
        plaintextHttpAllowed = builder.plaintextHttpAllowed;
    }

    /**
     * @param baseUri the Web Query endpoint, for example {@code https://ts.example.org:10443}; a path
     *     prefix such as {@code https://proxy.example.org/teamspeak/} is kept for reverse proxies
     * @param apiKey an API key created with ServerQuery {@code apikeyadd}
     * @return a builder with default timeouts and capacity
     * @throws IllegalArgumentException if the URI is not an absolute http(s) URI with a host and without
     *     user information, query or fragment, or if the key is blank or contains characters other than
     *     visible ASCII
     */
    public static Builder builder(URI baseUri, String apiKey) {
        return new Builder(baseUri, apiKey);
    }

    /** @return the Web Query endpoint, ending with {@code /} */
    public URI getBaseUri() {
        return baseUri;
    }

    String apiKey() {
        return apiKey;
    }

    /** @return the TCP and TLS connection deadline */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /** @return the deadline from sending a request until its response is complete */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /** @return the shutdown budget for aborting requests and terminating workers */
    public Duration getCloseTimeout() {
        return closeTimeout;
    }

    /** @return the maximum number of requests in flight, including their result processing */
    public int getRequestCapacity() {
        return requestCapacity;
    }

    /** @return the TLS context for HTTPS, or empty for the JDK default trust */
    public Optional<SSLContext> getSslContext() {
        return Optional.ofNullable(sslContext);
    }

    /** @return whether an {@code http} base URI is accepted */
    public boolean isPlaintextHttpAllowed() {
        return plaintextHttpAllowed;
    }

    @Override
    public String toString() {
        return "WebQueryConfig[baseUri=" + baseUri + "]";
    }

    /** Mutable, single-threaded builder of {@link WebQueryConfig}. */
    public static final class Builder {

        private final URI baseUri;
        private final String apiKey;
        private Duration connectTimeout = Duration.ofSeconds(4);
        private Duration requestTimeout = Duration.ofSeconds(4);
        private Duration closeTimeout = Duration.ofSeconds(4);
        private int requestCapacity = 16;
        private SSLContext sslContext;
        private boolean plaintextHttpAllowed;

        private Builder(URI baseUri, String apiKey) {
            this.baseUri = endpoint(Objects.requireNonNull(baseUri, "baseUri"));
            Objects.requireNonNull(apiKey, "apiKey");
            if (apiKey.isEmpty() || !apiKey.chars().allMatch(c -> c > 0x20 && c < 0x7f)) {
                throw new IllegalArgumentException("API key must be non-empty visible ASCII");
            }
            this.apiKey = apiKey;
        }

        private static URI endpoint(URI uri) {
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new IllegalArgumentException("Web Query URI must use http or https");
            }
            if (uri.isOpaque() || uri.getHost() == null) {
                throw new IllegalArgumentException("Web Query URI needs a host");
            }
            if (uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException(
                        "Web Query URI must not contain user information, query or fragment");
            }
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            if (!path.endsWith("/")) path += "/";
            return URI.create(scheme + "://" + uri.getRawAuthority() + path);
        }

        /**
         * Sets the TCP and TLS connection deadline. Defaults to four seconds.
         * @param value positive, finite duration
         * @return this builder
         */
        public Builder setConnectTimeout(Duration value) {
            connectTimeout = TS3Config.timeout(value);
            return this;
        }

        /**
         * Sets the deadline from sending a request until its response is complete. Defaults to four seconds.
         * An expired request may still have executed on the server.
         * @param value positive, finite duration
         * @return this builder
         */
        public Builder setRequestTimeout(Duration value) {
            requestTimeout = TS3Config.timeout(value);
            return this;
        }

        /**
         * Sets the shutdown budget for aborting requests and terminating workers. Defaults to four seconds.
         * @param value positive, finite duration
         * @return this builder
         */
        public Builder setCloseTimeout(Duration value) {
            closeTimeout = TS3Config.timeout(value);
            return this;
        }

        /**
         * Sets how many requests may be in flight at once; further requests fail with
         * {@link com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException}. Defaults to 16.
         * @param capacity positive request count
         * @return this builder
         */
        public Builder setRequestCapacity(int capacity) {
            if (capacity < 1) throw new IllegalArgumentException("Request capacity must be positive");
            requestCapacity = capacity;
            return this;
        }

        /**
         * Sets the TLS context used for HTTPS, for example one trusting a private certificate authority.
         * Hostname verification stays enabled. Defaults to the JDK trust store.
         * @param context the TLS context
         * @return this builder
         */
        public Builder setSslContext(SSLContext context) {
            sslContext = Objects.requireNonNull(context, "context");
            return this;
        }

        /**
         * Allows an {@code http} base URI. The API key and all data then travel unencrypted; use this
         * only for loopback, a private network or a TLS-terminating proxy on the same host.
         * @param allowed whether plaintext HTTP is accepted
         * @return this builder
         */
        public Builder setPlaintextHttpAllowed(boolean allowed) {
            plaintextHttpAllowed = allowed;
            return this;
        }

        /**
         * @return an immutable configuration snapshot
         * @throws IllegalArgumentException if the base URI uses {@code http} and plaintext is not allowed
         */
        public WebQueryConfig build() {
            if (baseUri.getScheme().equals("http") && !plaintextHttpAllowed) {
                throw new IllegalArgumentException(
                        "Plaintext HTTP exposes the API key; use https or call setPlaintextHttpAllowed(true)");
            }
            return new WebQueryConfig(this);
        }

        @Override
        public String toString() {
            return "WebQueryConfig.Builder[baseUri=" + baseUri + "]";
        }
    }
}
