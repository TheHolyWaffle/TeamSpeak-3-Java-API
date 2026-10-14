package com.github.theholywaffle.teamspeak3.api.wrapper;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class QueryError extends Wrapper {

    private static final int ERROR_ID_OK = 0;
    private static final int ERROR_ID_EMPTY_RESULT_SET = 1281;

    public QueryError(Map<String, String> map) {
        super(map);
    }

    public int getId() {
        return requireInt("id");
    }

    public String getMessage() {
        return require("msg");
    }

    public Optional<String> getExtraMessage() {
        return findText("extra_msg");
    }

    public OptionalInt getFailedPermissionId() {
        return findInt("failed_permid");
    }

    public boolean isSuccessful() {
        return isSuccessful(getId());
    }

    /**
     * Checks whether an error ID reports success: {@code 0} (ok) or {@code 1281} (empty result set).
     *
     * @param id the error ID
     * @return whether the ID reports success
     */
    public static boolean isSuccessful(int id) {
        return id == ERROR_ID_OK || id == ERROR_ID_EMPTY_RESULT_SET;
    }
}
