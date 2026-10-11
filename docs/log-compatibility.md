# Verified instance and virtual-server log retrieval

This focused #441 slice verifies TS3 **3.13.8 RAW**, TS3 **3.13.8 SSH** and TS6
**6.0.0-beta13.1 SSH**. Existing image pins, authentication and Docker CI are
unchanged; see [integration setup](integration-tests.md) and the
[overall command inventory](command-compatibility.md).

## Verified operations

`ServerQueryCompatibilityIT.logDomain` captures actual `logadd` and `logview`
help, then reads existing logs from the authenticated disposable fixtures:

- With no virtual server selected, `getInstanceLogEntries(100)` includes the
  connected server's version. The virtual-server getter reads the instance log
  in this context, including its `ServerLibPriv` startup category.
- After selecting server 1, `getVirtualServerLogEntries(100)` returns rows with
  its server-ID prefix and excludes the instance startup category. Explicit
  instance reads still return the instance category while a server is selected.
- A one-line request returns one row. Default and nonpositive requests return a
  nonempty list of up to 100 rows. All four sync/async count-taking methods reject
  requests above 100 synchronously with `IllegalArgumentException`; they do not
  clamp oversized counts to 100.
- Raw one-line output for each context contains `l`, `last_pos` and `file_size`;
  the decoded line is nonblank, position is nonnegative and size is positive.
  Typed getters return decoded `l` strings, without pagination metadata.
- Raw `logadd loglevel=5` fails with **1538 / `invalid parameter`** on every
  target. A later version command succeeds, preserving error/recovery behavior.

Reports at `target/compatibility/<target>-logs.txt` include live help, row counts,
response field names and error metadata. Arbitrary log content is excluded
because startup logs can contain generated privilege keys or other credentials.
Assertions inspect only known startup version/category/server-ID markers and do
not print log rows. Each target has a 60-second timeout, individual query commands
retain five-second deadlines, and its session closes on failure.

## Custom-write limitation discovered during probes

Successful custom writes are **not** in the accepted subset. TS3 INFO probes
preserved Unicode, pipe and literal backslash message suffixes. TS6 sometimes
returned the custom entry, but other runs returned success from `logadd` while
immediate reads or a five-second polling deadline did not observe the INFO entry.
An INFO/WARNING pair also failed to expose its WARNING entry within the deadline.
Repeated reads and an explicit `begin_pos=0` reset reported the same file size
without that marker; cursor resets did not establish a consistent remedy.

These observations do not establish the cause, a routing/configuration difference,
a universal flush rule or a safe visibility bound. No logging settings or runtime
implementation are changed to hide this behavior. Custom-write visibility and
flush semantics remain #441 acceptance work; the retained regression verifies
existing log retrieval and invalid-severity errors without a flaky positive-write
assertion or a longer timeout.

## Release intent and migration

Internal **`test:`** release intent for planned 2.0.0 acceptance. No runtime,
public API, dependency or default changes. Sync/async Javadocs now accurately
state the existing limit: nonpositive counts request the server default, while
counts above 100 throw instead of returning 100 entries.

Treat rows as server-formatted strings rather than typed timestamps. A returned
window can begin partway through a physical line; do not assume each row starts
with a complete date. Choose instance or selected-server context explicitly and
handle actual server errors. Use the raw response when position/size metadata or
unmodeled options are needed; line counts are not byte positions, and one request
does not guarantee an entire file. Successful `logadd` does not establish
immediate visibility through `logview` on the pinned TS6 server.

## Remaining scope and official sources

This covers serveradmin and existing-log retrieval on the pinned fixtures.
Successful custom writes/visibility, severity levels, denied roles, reverse order,
`begin_pos` continuation, large-file/rotation behavior, concurrent writers,
timestamp parsing, other logging settings and versions remain unverified.
No notification or voice-client guarantee is introduced. TS6 RAW and Web Query
remain unclaimed. Other #441 gaps remain in the inventory; file-transfer
reliability and NAT endpoints remain #442 scope.

Reviewed official [TS6 beta13.1 release notes](https://github.com/teamspeak/teamspeak6-server/releases/tag/v6.0.0-beta13.1),
[logging configuration](https://github.com/teamspeak/teamspeak6-server/blob/main/CONFIG.md#logging),
[JUnit 6.1.3](https://docs.junit.org/6.1.3/overview.html),
[Testcontainers readiness](https://java.testcontainers.org/features/startup_and_waits/)
and [Failsafe verification](https://maven.apache.org/surefire/maven-failsafe-plugin/usage.html).
Configuration documents log paths, append behavior and timestamp zones; actual
help/output and assertions establish the command subset above.
