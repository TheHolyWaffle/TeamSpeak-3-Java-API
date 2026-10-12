# TS3 file transfers

The library supports the TS3 file-transfer protocol (`proto=0`) with RAW or SSH
ServerQuery. **The separate file socket is plaintext TCP even when the query
connection uses SSH.** Transfer keys authorize that socket; they are not an
encryption layer. TS6/S3 transfers are not supported.

## Limits and endpoint mapping

Each query snapshots `TS3Config.getFileTransferConfig()`. Defaults are a four-second
TCP connect budget, four seconds per socket read, five minutes for the complete
data operation, and 16 MiB for direct-download and icon-buffer allocations. These
limits are independent of query connect, handshake and command-response budgets.
The complete data budget starts when transfer work begins, before endpoint
resolution, and also aborts blocked writes and caller stream operations. Icon
preparation and its subsequent file socket each have a data-operation budget;
query initialization between them has the ordinary command budget.

```java
var config = new TS3Config().setHost("query.example.net")
    .setFileTransferConfig(new FileTransferConfig(
        Duration.ofSeconds(4), Duration.ofSeconds(10), Duration.ofMinutes(2),
        32 * 1024 * 1024,
        (queryHost, advertisedHosts, advertisedPort) ->
            new InetSocketAddress("files.example.net", 43033)));
```

The resolver affects only file sockets. It receives the configured query host,
the complete advertised address field (possibly empty or comma separated), and
the advertised port. It must return promptly and be thread safe. The default
selects the first advertised address, including a single address without a comma;
empty addresses and exact IPv4/IPv6 wildcards fall back to the query host. It does
not probe arbitrary alternate addresses, guess NAT mappings or fall back after a
connection failure. Use an explicit mapping when the advertised endpoint is not
reachable. Testcontainers callers should use `container.getHost()` and
`container.getMappedPort(advertisedPort)` after starting the container.

Java/OS name resolution and caller code that ignores interruption cannot be
forcibly terminated. The total deadline publishes failure and closes any owned
socket, but admission remains occupied until the actual work returns. This also
applies to a misbehaving resolver. Cooperative implementations must stop on
interruption and must not wait indefinitely.

## Streams, lengths and completion

Streams belong to the caller. The library never closes either stream or flushes
the caller's output, on success, failure, cancellation or query shutdown. Keep a
stream open and exclusively available until work ends. Socket writes are flushed
internally. All copy buffers are fixed at 16 KiB, independent of OS socket buffers.

Uploads consume exactly the declared length; trailing source bytes remain unread.
Negative lengths fail before command submission. Premature EOF or a read making
no progress fails with `TS3FileTransferFailedException`. Downloads validate the
announced length, write exactly that many bytes (minus a requested offset), then
require socket EOF: truncation, surplus bytes and stalled termination fail. Caller
streams may contain a partial prefix after failure. Direct downloads and icon
stream buffering validate lengths against the configured limit before allocation.
Use streaming downloads for larger files. Already supplied byte arrays need no
library-side buffering, so the limit does not reject direct upload inputs.

Transfer-init rejection remains a `TS3CommandFailedException` with the actual
server error. Socket/copy/deadline errors become `TS3FileTransferFailedException`
with the I/O cause. A completed upload means the bytes were written to the socket;
the protocol supplies no final durability acknowledgment. Read file metadata or
download the bytes when server acceptance/completeness must be established.

## Cancellation and resume

Cancel the **original** async result (`cancel(false)` and `cancel(true)` both abort
the operation). Cancellation before submission removes the command; during an
in-flight initialization, its late response is consumed without opening a file
socket. Active cancellation closes the socket and interrupts the transfer worker.
`TS3Query.close()`/`exit()` abort owned transfers as part of shutdown. None of
these actions retracts already transferred bytes or deletes the partial file.
Caller streams are never closed to force cancellation. Uncooperative code can
retain admission even after the future is cancelled or fails its total deadline.

Cancelling a native dependent JDK stage does not cancel the original operation.
Timed `get` only bounds waiting; `orTimeout` changes the future outcome without
aborting transfer work. Configure the transfer deadline or cancel the original
result before it is already terminal.

Download continuation uses the overload
`downloadFile(output, path, channel, password, seekPosition)`. Position must be
nonnegative and no greater than the server file size. The caller positions its
output itself; the library writes only the suffix and returns bytes written.
An offset exactly at EOF writes zero bytes.

Upload continuation uses
`uploadFile(input, totalSize, path, false, channel, password, true)`. Supply the
**entire original source from its beginning**, not just the suffix. The server's
`seekpos` is validated against totalSize; that prefix is consumed locally and
only the remainder is sent. Resume and overwrite are mutually exclusive. There
is no automatic replay or retry: confirm that the retained prefix belongs to the
same file before explicitly resuming. The library does not verify prefix identity
or roll back partial uploads.

## Evidence and 2.0 migration

`FileTransferCompatibilityIT` verifies the public synchronous/asynchronous APIs on
the pinned TS3 **3.13.8 RAW and SSH**, using the actual Docker host/mapped port.
It checks 65,537-byte exact content, empty files, direct-memory rejection, missing
file error **2051**, existing-file rejection **2050**, download suffix/EOF offsets,
truncated upload and retained-prefix resume, active upload/download cancellation,
caller ownership and query recovery. Help/output and server version/image evidence
are written to `target/compatibility/*-reliable-files*.txt`; transfer keys are not
recorded. This is evidence for those combinations, not other TS3 versions or
arbitrary NAT layouts.

Loopback tests independently check single/list/wildcard/IPv6 host selection,
negative and malformed lengths/seek positions, premature/surplus EOF, nonprogress,
TCP backlog connect timeout, read/total deadlines, blocked writes, pre-init/active
cancellation, late-response alignment, bounded icon preparation, shutdown and
admission retained by uncooperative caller code. Existing command/content/icon
compatibility tests remain part of the full suite.

Breaking changes for 2.0: finite file deadlines, a default 16 MiB in-memory limit,
terminal cancellation that actually aborts file I/O, and asynchronous icon stream
buffering whose failures arrive through the returned future. Configure appropriate
limits explicitly, migrate large direct downloads to caller-owned outputs, and
cancel the original API future. SSH query encryption has never protected this
separate file socket.

Implementation follows [Java 25 Socket connect/read/close contracts](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/Socket.html)
and [Testcontainers mapped endpoint guidance](https://java.testcontainers.org/features/networking/).
