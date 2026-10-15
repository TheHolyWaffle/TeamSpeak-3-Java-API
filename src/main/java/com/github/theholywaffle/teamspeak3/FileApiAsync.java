package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.FileLocation;
import com.github.theholywaffle.teamspeak3.api.IconId;
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
import java.util.concurrent.Executor;
import java.util.function.Function;

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
        return requests.models(FileCommands.ftGetFileList(directory), FileListEntry::new);
    }

    /** @see FileApi#info(FileLocation) */
    public CompletableFuture<FileInfo> info(FileLocation file) {
        return requests.model(FileCommands.ftGetFileInfo(List.of(file)), FileInfo::new);
    }

    /** @see FileApi#info(List) */
    public CompletableFuture<List<FileInfo>> info(List<FileLocation> files) {
        return requests.models(FileCommands.ftGetFileInfo(files), FileInfo::new);
    }

    /** @see FileApi#createDirectory(FileLocation) */
    public CompletableFuture<Void> createDirectory(FileLocation directory) {
        return requests.run(FileCommands.ftCreateDir(directory));
    }

    /** @see FileApi#delete(FileLocation) */
    public CompletableFuture<Void> delete(FileLocation file) {
        return delete(List.of(file));
    }

    /** @see FileApi#delete(List) */
    public CompletableFuture<Void> delete(List<FileLocation> files) {
        return requests.run(FileCommands.ftDeleteFile(files));
    }

    /** @see FileApi#move(FileLocation, FileLocation) */
    public CompletableFuture<Void> move(FileLocation from, FileLocation to) {
        return requests.run(FileCommands.ftRenameFile(from, to));
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
        return executeTransfer(
                helper -> FileCommands.ftInitUpload(
                        helper.getClientTransferId(), target, length, mode == UploadMode.OVERWRITE, resume),
                "Upload failed",
                (helper, params, operation) -> {
                    helper.uploadFile(data, length, params, resume, operation);
                    return null;
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
        return executeTransfer(
                helper -> FileCommands.ftInitDownload(helper.getClientTransferId(), file, offset),
                "Download failed",
                (helper, params, operation) -> {
                    helper.downloadFile(out, params, offset, operation);
                    return params.getFileSize() - offset;
                });
    }

    /** @see FileApi#downloadBytes(FileLocation) */
    public CompletableFuture<byte[]> downloadBytes(FileLocation file) {
        return executeTransfer(
                helper -> FileCommands.ftInitDownload(helper.getClientTransferId(), file, 0),
                "Download failed",
                FileTransferHelper::downloadBytes);
    }

    /** @see FileApi#icons() */
    public CompletableFuture<List<IconFile>> icons() {
        return CommandFutures.map(
                list(FileLocation.ofServer("/icons/")),
                files -> files.stream()
                        .filter(file -> !file.isDirectory() && !file.isStillUploading())
                        .map(file -> new IconFile(file.getMap()))
                        .toList());
    }

    /** @see FileApi#uploadIcon(InputStream, long) */
    public CompletableFuture<IconId> uploadIcon(InputStream data, long length) {
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
                    files.callbacks(),
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
    public CompletableFuture<IconId> uploadIcon(byte[] data) {
        IconId iconId = FileTransferHelper.getIconId(data);
        return CommandFutures.map(upload(iconLocation(iconId), data, UploadMode.OVERWRITE), _ -> iconId);
    }

    /** @see FileApi#downloadIcon(IconId, OutputStream) */
    public CompletableFuture<Long> downloadIcon(IconId iconId, OutputStream out) {
        return download(iconLocation(iconId), out);
    }

    /** @see FileApi#downloadIcon(IconId) */
    public CompletableFuture<byte[]> downloadIcon(IconId iconId) {
        return downloadBytes(iconLocation(iconId));
    }

    /** @see FileApi#deleteIcons(IconId...) */
    public CompletableFuture<Void> deleteIcons(IconId... iconIds) {
        return delete(Arrays.stream(iconIds).map(FileApiAsync::iconLocation).toList());
    }

    private static FileLocation iconLocation(IconId iconId) {
        return FileLocation.ofServer(iconId.path());
    }

    @FunctionalInterface
    private interface FileWork<T> {
        T run(FileTransferHelper.Operation operation) throws IOException;
    }

    @FunctionalInterface
    private interface TransferWork<T> {
        T run(FileTransferHelper helper, FileTransferParameters params, FileTransferHelper.Operation operation)
                throws IOException;
    }

    private <T> CompletableFuture<T> executeTransfer(
            Function<FileTransferHelper, Command> request, String failureMessage, TransferWork<T> transfer) {
        return requests.backend().withFileTransfers(files -> executeTransfer(files, request, failureMessage, transfer));
    }

    private <T> CompletableFuture<T> executeTransfer(
            FileTransfers files,
            Function<FileTransferHelper, Command> request,
            String failureMessage,
            TransferWork<T> transfer) {
        Command command = request.apply(files.helper());
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
                    files.callbacks(),
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
                        return transfer.run(files.helper(), params, op);
                    },
                    () -> finished.complete(null));
        });
        requests.backend().enqueue(command, finished);
        return result;
    }

    private static CompletableFuture<Void> abortOnCancel(FileTransferHelper.Operation operation) {
        var cancellation = new CompletableFuture<Void>();
        var _ = cancellation.whenComplete((_, _) -> operation.abort());
        return cancellation;
    }

    private static <T> void runFileWork(
            Executor callbacks,
            FileTransferHelper.Operation operation,
            CompletableFuture<T> work,
            String abortMessage,
            String failureMessage,
            FileWork<T> body,
            Runnable onFinish) {
        try {
            callbacks.execute(() -> {
                T value = null;
                Throwable failure = null;
                try (operation) {
                    operation.start(() -> callbacks.execute(() -> work.completeExceptionally(
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
