package com.github.theholywaffle.teamspeak3.api;

import com.github.theholywaffle.teamspeak3.ChannelApi;
import com.github.theholywaffle.teamspeak3.VirtualServerApi;
import java.util.Map;

/**
 * Describes how the virtual server manages audio encryption.
 * <p>
 * Can be edited with {@link VirtualServerApi#edit(Map)}.
 * </p>
 */
public enum CodecEncryptionMode {

    /**
     * Each channel manages audio encryption on its own.
     *
     * @see ChannelApi#edit(ChannelId, Map)
     */
    CODEC_CRYPT_INDIVIDUAL(0),

    /**
     * Audio encryption is globally disabled on this virtual server.
     */
    CODEC_CRYPT_DISABLED(1),

    /**
     * Audio encryption is globally enabled on this virtual server.
     */
    CODEC_CRYPT_ENABLED(2),

    /**
     * An unknown codec encryption mode.
     * <p>
     * If you ever encounter an unknown mode, please tell us!
     * We may have to update the API.
     * </p>
     */
    UNKNOWN(-1);

    private static final CodecEncryptionMode[] VALUES = values();

    private final int i;

    CodecEncryptionMode(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static CodecEncryptionMode fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, CodecEncryptionMode::getIndex, index, UNKNOWN);
    }
}
