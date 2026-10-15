package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.PermissionGroupType;
import java.util.Map;

/**
 * Where a single permission is assigned, as returned by permission searches ({@code permfind}).
 * <p>
 * To reduce bandwidth usage, the TS3 server only transmits the numeric
 * permission ID and not the permission name.
 * </p><p>
 * The {@code doc} directory of a TS3 server installation describes the permission system.
 * </p>
 */
public class PermissionSource extends Wrapper {

    public PermissionSource(Map<String, String> map) {
        super(map);
    }

    /**
     * Where this permission assignment originates from.
     *
     * @return the type of this permission assignment, or {@link PermissionGroupType#UNKNOWN} for a type this library does not know
     */
    public PermissionGroupType getType() {
        return requireCode("t", PermissionGroupType::fromIndex);
    }

    /**
     * Specifies where this permission assignment originates from,
     * depending on the {@linkplain #getType() type} of this assignment.
     * <p>
     * {@code x -> y} := <i>In case {@code type} is {@code x}, {@code majorId} means {@code y}</i>
     * </p><p>
     * {@linkplain PermissionGroupType#SERVER_GROUP SERVER_GROUP} {@code ->} {@linkplain ServerGroup#getId() Server group ID}<br>
     * {@linkplain PermissionGroupType#GLOBAL_CLIENT GLOBAL_CLIENT} {@code ->} {@linkplain Client#getDatabaseId() Client database ID}<br>
     * {@linkplain PermissionGroupType#CHANNEL CHANNEL} {@code ->} {@linkplain Channel#getId() Channel ID}<br>
     * {@linkplain PermissionGroupType#CHANNEL_GROUP CHANNEL_GROUP} {@code ->} {@linkplain Channel#getId() Channel ID}<br>
     * {@linkplain PermissionGroupType#CHANNEL_CLIENT CHANNEL_CLIENT} {@code ->} {@linkplain Channel#getId() Channel ID}
     * </p>
     *
     * @return the major ID of the source of this assignment as described above
     */
    public long getMajorId() {
        return requireLong("id1");
    }

    /**
     * Specifies where this permission assignment originates from,
     * depending on the {@linkplain #getType() type} of this assignment.
     * <p>
     * {@code x -> y} := <i>In case {@code type} is {@code x}, {@code minorId} means {@code y}</i>
     * </p><p>
     * {@linkplain PermissionGroupType#CHANNEL_GROUP CHANNEL_GROUP} {@code ->} {@linkplain ChannelGroup#getId() Channel group ID}<br>
     * {@linkplain PermissionGroupType#CHANNEL_CLIENT CHANNEL_CLIENT} {@code ->} {@linkplain Client#getDatabaseId() Client database ID}
     * </p><p>
     * Otherwise TeamSpeak sends {@code 0}.
     * </p>
     *
     * @return the minor ID of the source of this assignment as described above
     */
    public long getMinorId() {
        return requireLong("id2");
    }

    /**
     * Gets the numerical permission ID supplied by the connected server.
     * Resolve it using that server's permission metadata, not an embedded enum ID.
     *
     * @return this permission's numerical ID
     */
    public int getId() {
        return requireInt("p");
    }
}
