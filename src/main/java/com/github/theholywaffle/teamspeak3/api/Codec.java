package com.github.theholywaffle.teamspeak3.api;

import com.github.theholywaffle.teamspeak3.ChannelApi;
import java.util.Map;

/**
 * Voice codecs currently used by TeamSpeak3.
 * <p>
 * A channel's codec may be edited by using {@link ChannelApi#edit(ChannelId, Map)}.
 * All voice data may also be encrypted.
 * </p>
 */
public enum Codec {

    /**
     * The Speex narrowband codec with a sampling rate of 8kHz.
     * <p>
     * All Speex codecs are basically obsolete as Opus is better
     * in every aspect and has a comparable required bandwidth.
     * </p><ul>
     * <li>Bandwidth per client with codec quality 0: 2.49 KiB/s</li>
     * <li>Bandwidth per client with codec quality 10: 5.22 KiB/s</li>
     * </ul>
     */
    SPEEX_NARROWBAND(0),

    /**
     * The Speex wideband codec with a sampling rate of 16kHz.
     * <p>
     * All Speex codecs are basically obsolete as Opus is better
     * in every aspect and has a comparable required bandwidth.
     * </p><ul>
     * <li>Bandwidth per client with codec quality 0: 2.69 KiB/s</li>
     * <li>Bandwidth per client with codec quality 10: 7.37 KiB/s</li>
     * </ul>
     */
    SPEEX_WIDEBAND(1),

    /**
     * The Speex ultra-wideband codec with a sampling rate of 32kHz.
     * <p>
     * All Speex codecs are basically obsolete as Opus is better
     * in every aspect and has a comparable required bandwidth.
     * </p><ul>
     * <li>Bandwidth per client with codec quality 0: 2.73 KiB/s</li>
     * <li>Bandwidth per client with codec quality 10: 7.57 KiB/s</li>
     * </ul>
     */
    SPEEX_ULTRAWIDEBAND(2),

    /**
     * The CELT Mono codec. It is optimised for music but still generates very good
     * results in voice transmission.
     * <p>
     * Note that this codec requires the most bandwidth of all currently available codecs.
     * </p><ul>
     * <li>Bandwidth per client with codec quality 0: 6.10 KiB/s</li>
     * <li>Bandwidth per client with codec quality 10: 13.92 KiB/s</li>
     * </ul>
     */
    CELT_MONO(3),

    /**
     * The Opus codec optimised for voice transmission.
     * <p>
     * This is the default codec used by TeamSpeak and usually achieves the
     * best results for voice transmission.
     * </p><ul>
     * <li>Bandwidth per client with codec quality 0: 2.73 KiB/s</li>
     * <li>Bandwidth per client with codec quality 10: 7.71 KiB/s</li>
     * </ul>
     */
    OPUS_VOICE(4),

    /**
     * The Opus codec optimised for music transmission.
     * <p>
     * Please note that if used only to transmit speech, this codec can
     * sound worse than Opus Voice despite a higher bandwidth.
     * </p><ul>
     * <li>Bandwidth per client with codec quality 0: 3.08 KiB/s</li>
     * <li>Bandwidth per client with codec quality 10: 11.87 KiB/s</li>
     * </ul>
     */
    OPUS_MUSIC(5),

    /**
     * An unknown codec.
     * <p>
     * If you ever encounter an unknown codec, please tell us!
     * We may have to update the API.
     * </p>
     */
    UNKNOWN(-1);

    private static final Codec[] VALUES = values();

    private final int i;

    Codec(int i) {
        this.i = i;
    }

    public int getIndex() {
        return i;
    }

    /**
     * @param index the ServerQuery code
     * @return the matching constant, or {@link #UNKNOWN} for a code this library does not know
     */
    public static Codec fromIndex(int index) {
        return CodeLookup.byIndex(VALUES, Codec::getIndex, index, UNKNOWN);
    }
}
