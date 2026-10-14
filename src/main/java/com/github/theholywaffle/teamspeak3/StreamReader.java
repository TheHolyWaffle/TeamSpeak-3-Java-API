package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.CommandTrait;
import com.github.theholywaffle.teamspeak3.commands.ProtocolRedactor;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;
import com.github.theholywaffle.teamspeak3.commands.response.ResponseBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StreamReader implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(StreamReader.class);

    private final TS3Query ts3;
    private final Connection con;
    private final ProtocolLineReader in;
    private final boolean logComms;

    private CommandQueue commandQueue = null;
    private ResponseBuilder responseBuilder = null;
    private boolean receivingResponses;

    StreamReader(Connection connection, InputStream inStream, TS3Query query, QueryConfig config) throws IOException {
        ts3 = query;
        con = connection;
        in = new ProtocolLineReader(inStream);
        logComms = config.getEnableCommunicationsLogging();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String line;

            try {
                // Will block until a full line of text could be read.
                line = in.readLine();
            } catch (SocketTimeoutException socketTimeout) {
                // Really disconnected or just no data transferred for <commandTimeout> milliseconds?
                if (con.isTimedOut()) {
                    log.error("Connection timed out.", socketTimeout);
                    break;
                } else {
                    continue;
                }
            } catch (IOException io) {
                if (!Thread.currentThread().isInterrupted()) {
                    log.error("Connection error occurred.", io);
                }
                break;
            }

            if (line == null) {
                // End of stream: connection terminated by server
                log.error("Connection closed by the server.");
                break;
            } else if (line.isEmpty()) {
                continue; // The server is sending garbage
            }

            if (line.startsWith("notify")) {
                handleEvent(line);
            } else if (receivingResponses || line.startsWith("error ") || startsWithField(line)) {
                // Welcome text is informational, not a fixed-size response frame.
                // Protocol data is key/value text; error lines terminate a command.
                receivingResponses = true;
                // Response fragments never extend a command deadline.
                try {
                    handleCommandResponse(line);
                } catch (IllegalArgumentException malformed) {
                    log.error("Malformed command response: {}", ProtocolRedactor.redactLine(line), malformed);
                    break;
                }
            } else {
                logReceived("welcome", line);
            }
        }

        try {
            in.close();
        } catch (IOException ignored) {
            // Ignore
        }

        if (!Thread.currentThread().isInterrupted()) {
            con.internalDisconnect();
        }
    }

    private static boolean startsWithField(String line) {
        int equals = line.indexOf('=');
        int space = line.indexOf(' ');
        return equals > 0 && (space == -1 || equals < space);
    }

    private void handleEvent(String event) {
        logReceived("event", event);

        String[] parts = event.split(" ", 2);
        if (parts.length == 2) ts3.getEventManager().fireEvent(parts[0], parts[1]);
        else ts3.getEventManager().malformedNotification();
    }

    private void handleCommandResponse(String response) {
        if (responseBuilder == null) {
            commandQueue = con.getCommandQueue();
            Command command = commandQueue.peekReceiveQueue();

            if (command == null) {
                log.warn("[UNHANDLED] < {}", ProtocolRedactor.redactLine(response));
                return;
            }

            responseBuilder = new ResponseBuilder(command);
        }

        logReceived(responseBuilder.getCommand().getName(), response);

        if (response.startsWith("error ")) {
            handleCommandError(responseBuilder, response);

            responseBuilder = null;
        } else {
            responseBuilder.appendResponse(response);
        }
    }

    private void handleCommandError(ResponseBuilder responseBuilder, String error) {
        Command command = responseBuilder.getCommand();
        if (command.getTraits().contains(CommandTrait.ENDS_CONNECTION)) {
            Thread.currentThread().interrupt();
        }

        QueryError queryError = DefaultArrayResponse.parseError(error);
        CompletableFuture<DefaultArrayResponse> future = command.getFuture();

        if (queryError.isSuccessful()) {
            DefaultArrayResponse response = responseBuilder.buildResponse();

            commandQueue.completeResponse(command, con, () -> future.complete(response));
        } else {
            log.debug("TS3 command error: {}", queryError);

            commandQueue.completeResponse(
                    command,
                    con,
                    () -> future.completeExceptionally(new TS3CommandFailedException(queryError, command.getName())));
        }
    }

    private void logReceived(String name, String line) {
        if (logComms && log.isDebugEnabled()) log.debug("[{}] < {}", name, ProtocolRedactor.redactLine(line));
    }
}
