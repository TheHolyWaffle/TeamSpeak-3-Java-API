package com.github.theholywaffle.teamspeak3.api;

/** How an upload treats an existing file at the target location. */
public enum UploadMode {
    /** Fail with server error 2050 if the file exists. */
    CREATE,
    /** Replace an existing file. */
    OVERWRITE,
    /**
     * Continue a partial upload. The source stream must supply the complete original content;
     * bytes already on the server are skipped. The existing prefix is not verified.
     */
    RESUME
}
