package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.commands.Command;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StreamWriter implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(StreamWriter.class);

    private final Connection con;
    private final BufferedWriter out;
    private final int floodRate;
    private final boolean logComms;

    StreamWriter(Connection connection, OutputStream outStream, QueryConfig config) {
        con = connection;
        out = new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
        floodRate = config.getFloodRate().getMs();
        logComms = config.getEnableCommunicationsLogging();
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (floodRate > 0) Thread.sleep(floodRate);

                Command command = con.getCommandQueue().transferCommand(con);
                if (command == null) continue;

                con.resetIdleTime();
                String message = command.encoded();

                // Command.toString() redacts, and only runs when DEBUG is enabled.
                if (logComms) log.debug("[{}] > {}", command.getName(), command);
                out.write(message);
                out.write("\n");
                out.flush();
            }
        } catch (IOException e) {
            if (!Thread.currentThread().isInterrupted()) {
                log.debug("Write failed", e);
                con.internalDisconnect();
            }
        } catch (RuntimeException e) {
            log.warn("Command encoding or write failed", e);
            con.internalDisconnect();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt(); // Regular shutdown
        }
    }
}
