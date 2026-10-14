package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.UploadMode;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3FileTransferFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueueFullException;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileInfo;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileListEntry;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransfer;
import com.github.theholywaffle.teamspeak3.api.wrapper.FileTransferParameters;
import com.github.theholywaffle.teamspeak3.api.wrapper.IconFile;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.FileCommands;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous file repository access, file transfers and icons.
 * <p>
 * Cancelling an original transfer result closes its plaintext file socket and interrupts its worker,
 * leaving caller streams open. {@link FileTransferConfig} bounds socket I/O and in-memory downloads.
 * SSH query encryption does not encrypt the separate file socket.
 * </p>
 *
 * @see FileApi the synchronous form and full documentation
 */
public final class FileApiAsync {

    private final QueryRequests requests;

    FileApiAsync(QueryRequests requests) {
        this.requests = requests;
    }

    /** @see FileApi#list(FileLocation) */
    public CompletableFuture<List<FileListEntry>> list(FileLocation directory) {
        return requests.models(
                FileCommands.ftGetFileList(directory.path(), directory.channelId(), directory.channelPassword()),
                FileListEntry::new);
    }

    /** @see FileApi#info(FileLocation) */
    public CompletableFuture<FileInfo> info(FileLocation file) {
        return requests.model(
                FileCommands.ftGetFileInfo(file.channelId(), file.channelPassword(), file.path()), FileInfo::new);
    }

    /** @see FileApi#info(List) */
    public CompletableFuture<List<FileInfo>> info(List<FileLocation> files) {
        int[] channels = files.stream().mapToInt(FileLocation::channelId).toArray();
        String[] passwords = files.stream().map(FileLocation::channelPassword).toArray(String[]::new);
        String[] paths = files.stream().map(FileLocation::path).toArray(String[]::new);
        return requests.models(FileCommands.ftGetFileInfo(channels, passwords, paths), FileInfo::new);
    }

    /** @see FileApi#createDirectory(FileLocation) */
    public CompletableFuture<Void> createDirectory(FileLocation directory) {
        return requests.run(
                FileCommands.ftCreateDir(directory.path(), directory.channelId(), directory.channelPassword()));
    }

    /** @see FileApi#delete(FileLocation) */
    public CompletableFuture<Void> delete(FileLocation file) {
        return delete(List.of(file));
    }

    /** @see FileApi#delete(List) */
    public CompletableFuture<Void> delete(List<FileLocation> files) {
        if (files.isEmpty()) throw new IllegalArgumentException("At least one file is required");
        FileLocation first = files.getFirst();
        for (FileLocation file : files) {
            if (file.channelId() != first.channelId()
                    || !Objects.equals(file.channelPassword(), first.channelPassword())) {
                throw new IllegalArgumentException("Files deleted together must share a channel and password");
            }
        }
        String[] paths = files.stream().map(FileLocation::path).toArray(String[]::new);
        return requests.run(FileCommands.ftDeleteFile(first.channelId(), first.channelPassword(), paths));
    }

    /** @see FileApi#move(FileLocation, FileLocation) */
    public CompletableFuture<Void> move(FileLocation from, FileLocation to) {
        Command command = from.channelId() == to.channelId()
                ? FileCommands.ftRenameFile(from.path(), to.path(), from.channelId(), from.channelPassword())
                : FileCommands.ftRenameFile(
                        from.path(),
                        to.path(),
                        from.channelId(),
                        from.channelPassword(),
                        to.channelId(),
                        to.channelPassword());
        return requests.run(command);
    }

    /** @see FileApi#transfers() */
    public CompletableFuture<List<FileTransfer>> transfers() {
        return requests.models(FileCommands.ftList(), FileTransfer::new);
    }

    /** @see FileApi#upload(FileLocation, InputStream, long, UploadMode) */
    public CompletableFuture<Void> upload(FileLocation target, InputStream data, long length, UploadMode mode) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(mode, "mode");
        boolean resume = mode == UploadMode.RESUME;
        return requests.backend().withFileTransfers(files -> {
            FileTransferHelper helper = files.helper();
            Command command = FileCommands.ftInitUpload(
                    helper.getClientTransferId(),
                    target.path(),
                    target.channelId(),
                    target.channelPassword(),
                    length,
                    mode == UploadMode.OVERWRITE,
                    resume);
            return executeTransfer(files, command, "Upload failed", (params, operation) -> {
                helper.uploadFile(data, length, params, resume, operation);
                return null;
            });
        });
    }

    /** @see FileApi#upload(FileLocation, byte[], UploadMode) */
    public CompletableFuture<Void> upload(FileLocation target, byte[] data, UploadMode mode) {
        return upload(target, new ByteArrayInputStream(data), data.length, mode);
    }

    /** @see FileApi#download(FileLocation, OutputStream) */
    public CompletableFuture<Long> download(FileLocation file, OutputStream out) {
        return download(file, out, 0);
    }

    /** @see FileApi#download(FileLocation, OutputStream, long) */
    public CompletableFuture<Long> download(FileLocation file, OutputStream out, long offset) {
        Objects.requireNonNull(out, "out");
        return requests.backend().withFileTransfers(files -> {
            FileTransferHelper helper = files.helper();
            Command command = FileCommands.ftInitDownload(
                    helper.getClientTransferId(), file.path(), file.channelId(), file.channelPassword(), offset);
            return executeTransfer(files, command, "Download failed", (params, operation) -> {
                helper.downloadFile(out, params, offset, operation);
                return params.getFileSize() - offset;
            });
        });
    }

    /** @see FileApi#downloadBytes(FileLocation) */
    public CompletableFuture<byte[]> downloadBytes(FileLocation file) {
        return requests.backend().withFileTransfers(files -> {
            FileTransferHelper helper = files.helper();
            Command command = FileCommands.ftInitDownload(
                    helper.getClientTransferId(), file.path(), file.channelId(), file.channelPassword(), 0);
            return executeTransfer(files, command, "Download failed", helper::downloadBytes);
        });
    }

    /** @see FileApi#icons() */
    public CompletableFuture<List<IconFile>> icons() {
        return CommandFutures.map(
                list(FileLocation.of(0, "/icons/")),
                files -> files.stream()
                        .filter(file -> !file.isDirectory() && !file.isStillUploading())
                        .map(file -> new IconFile(file.getMap()))
                        .toList());
    }

    /** @see FileApi#uploadIcon(InputStream, long) */
    public CompletableFuture<Long> uploadIcon(InputStream data, long length) {
        Objects.requireNonNull(data, "data");
        if (length < 0) throw new IllegalArgumentException("Negative icon length");
        return requests.backend().withFileTransfers(files -> {
            var admission = files.admission();
            if (!admission.tryAcquire()) return CompletableFuture.failedFuture(new TS3QueueFullException());
            var helper = files.helper();
            var operation = helper.newOperation();
            var work = new CompletableFuture<byte[]>();
            var preparation = CommandFutures.link(work, abortOnCancel(operation));
            runFileWork(
                    files,
                    operation,
                    work,
                    "Icon buffering aborted",
                    "Icon buffering failed",
                    op -> helper.bufferFully(data, length, op),
                    admission::release);
            return CommandFutures.compose(preparation, this::uploadIcon);
        });
    }

    /** @see FileApi#uploadIcon(byte[]) */
    public CompletableFuture<Long> uploadIcon(byte[] data) {
        long iconId = FileTransferHelper.getIconId(data);
        var upload = CommandFutures.map(upload(iconLocation(iconId), data, UploadMode.CREATE), ignored -> iconId);
        return QueryRequests.recover(upload, QueryRequests.FILE_EXISTS, iconId);
    }

    /** @see FileApi#downloadIcon(long, OutputStream) */
    public CompletableFuture<Long> downloadIcon(long iconId, OutputStream out) {
        return download(iconLocation(iconId), out);
    }

    /** @see FileApi#downloadIcon(long) */
    public CompletableFuture<byte[]> downloadIcon(long iconId) {
        return downloadBytes(iconLocation(iconId));
    }

    /** @see FileApi#deleteIcons(long...) */
    public CompletableFuture<Void> deleteIcons(long... iconIds) {
        return delete(
                Arrays.stream(iconIds).mapToObj(FileApiAsync::iconLocation).toList());
    }

    private static FileLocation iconLocation(long iconId) {
        return FileLocation.of(0, "/icon_" + iconId);
    }

    @FunctionalInterface
    private interface FileWork<T> {
        T run(FileTransferHelper.Operation operation) throws IOException;
    }

    @FunctionalInterface
    private interface TransferWork<T> {
        T run(FileTransferParameters params, FileTransferHelper.Operation operation) throws IOException;
    }

    private <T> CompletableFuture<T> executeTransfer(
            QueryBackend.FileTransfers files, Command command, String failureMessage, TransferWork<T> transfer) {
        var finished = new CompletableFuture<Void>();
        var work = new CompletableFuture<T>();
        var operation = files.helper().newOperation();
        var result = CommandFutures.link(work, abortOnCancel(operation), command.getFuture());
        var _ = command.getFuture().whenComplete((response, failure) -> {
            if (failure != null) {
                operation.close();
                work.completeExceptionally(failure);
                finished.complete(null);
                return;
            }
            runFileWork(
                    files,
                    operation,
                    work,
                    "File transfer aborted",
                    failureMessage,
                    op -> {
                        var row = response.getFirstResponse()
                                .orElseThrow(
                                        () -> new IOException(command.getName() + " returned no transfer parameters"));
                        var params = new FileTransferParameters(row.getMap());
                        var error = FileTransferHelper.field(params::findFailure);
                        if (error.isPresent()) throw new TS3CommandFailedException(error.get(), command.getName());
                        return transfer.run(params, op);
                    },
                    () -> finished.complete(null));
        });
        requests.backend().enqueue(command, finished);
        return result;
    }

    private static CompletableFuture<Void> abortOnCancel(FileTransferHelper.Operation operation) {
        var cancellation = new CompletableFuture<Void>();
        var _ = cancellation.whenComplete((value, failure) -> operation.abort());
        return cancellation;
    }

    private static <T> void runFileWork(
            QueryBackend.FileTransfers files,
            FileTransferHelper.Operation operation,
            CompletableFuture<T> work,
            String abortMessage,
            String failureMessage,
            FileWork<T> body,
            Runnable onFinish) {
        try {
            files.callbacks().execute(() -> {
                T value = null;
                Throwable failure = null;
                try (operation) {
                    operation.start(() -> files.callbacks()
                            .execute(() -> work.completeExceptionally(
                                    new TS3FileTransferFailedException(abortMessage, operation.abortCause()))));
                    value = body.run(operation);
                } catch (IOException exception) {
                    failure = new TS3FileTransferFailedException(failureMessage, operation.failureCause(exception));
                } catch (Throwable exception) {
                    failure = exception;
                } finally {
                    onFinish.run();
                }
                if (failure == null) work.complete(value);
                else work.completeExceptionally(failure);
            });
        } catch (RuntimeException rejected) {
            operation.close();
            onFinish.run();
            work.completeExceptionally(rejected);
        }
    }
}
