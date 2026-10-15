package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.VirtualServerId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * On-demand, read-only command discovery against a disposable TS3 instance.
 * <p>Run {@code ./gradlew auditServerQueryCommands}. This standalone task runs no tests and is
 * separate from {@code build}, {@code check}, and {@code integrationTest}. It reuses the server
 * fixture, accepting the TeamSpeak server license for the disposable container.</p>
 * <p>By default the task uses RAW and the compatibility suite's pinned image. Override the protocol
 * with {@code -PauditProtocol=SSH}, or the Docker image with {@code -PauditImage=teamspeak:TAG}.</p>
 * <p>The task saves a coverage report and unmodified live help to
 * {@code build/serverquery-audit/PROTOCOL/}. Missing builders are reported rather than causing a
 * test failure; aliases are identified from live help. The inventory compares command names with
 * literal builders, not every parameter or public facade.</p>
 */
public final class ServerQueryCommandAudit {
    private ServerQueryCommandAudit() {}

    /**
     * @param args output directory, command source directory, protocol, and optional Docker image
     * @throws Exception if the server or source inventory cannot be read
     */
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Path sources = Path.of(args[1]);
        var protocol = TS3Query.Protocol.valueOf(args[2].toUpperCase(Locale.ROOT));
        String image = args[3].isEmpty() ? TeamSpeakContainer.TS3_IMAGE : args[3];
        var directory = output.resolve(protocol.name());
        Files.createDirectories(directory);
        var builders = new TreeMap<String, String>();
        var builderPattern = Pattern.compile("new CommandBuilder\\(\"([a-z0-9]+)\"");
        try (var files = Files.list(sources)) {
            for (var file : files.filter(path -> path.getFileName().toString().endsWith("Commands.java"))
                    .sorted()
                    .toList()) {
                builderPattern
                        .matcher(Files.readString(file))
                        .results()
                        .forEach(match ->
                                builders.put(match.group(1), file.getFileName().toString()));
            }
        }
        if (builders.isEmpty()) throw new IllegalStateException("No literal command builders found in " + sources);

        try (var server = new TeamSpeakContainer(image)) {
            server.start();
            try (var query = new TS3Query(server.config(protocol))) {
                query.connect();
                var api = query.getApi();
                var version = api.instance().version();
                api.selectVirtualServerById(VirtualServerId.of(1));
                String overview = api.executeRawCommand("help").getRawResponse();
                Files.writeString(directory.resolve("overview.txt"), overview);
                var commands = Pattern.compile("(?:^|\\|)   ([a-z][a-z0-9]+)\\s+\\|")
                        .matcher(overview)
                        .results()
                        .map(match -> match.group(1))
                        .sorted()
                        .toList();
                if (commands.isEmpty()) throw new IllegalStateException("Could not parse the live command overview");
                var report = new ArrayList<String>();
                report.add("# Live TS3 command inventory\n");
                report.add("Image: `" + image + "`\n");
                report.add("Version: `" + version + "`; protocol: " + protocol + ".\n");
                report.add("Advertised: " + commands.size() + "; literal command builders: " + builders.size() + ".\n");
                report.add(
                        "This inventories command names, not every parameter or public facade. Raw live help is saved alongside this report.\n");
                report.add("| Command | Coverage |\n|---|---|");
                var missing = new TreeSet<String>();
                var aliasPattern = Pattern.compile("(?i)alias (?:for|of) ([a-z][a-z0-9]+)");
                for (String command : commands) {
                    String help = api.executeRawCommand("help " + command).getRawResponse();
                    if (help.isBlank()) throw new IllegalStateException("Empty live help for " + command);
                    Files.writeString(directory.resolve(command + ".txt"), help);
                    String coverage = builders.get(command);
                    if (coverage == null) {
                        var alias = aliasPattern.matcher(help);
                        if (alias.find() && builders.containsKey(alias.group(1))) {
                            coverage = "Alias of implemented `" + alias.group(1) + "`";
                        } else if (command.equals("help")) {
                            coverage = "Diagnostic text available through executeRawCommand";
                        } else {
                            coverage = "MISSING dedicated builder";
                            missing.add(command);
                        }
                    }
                    report.add("| `" + command + "` | " + coverage + " |");
                }
                var unadvertised = new TreeSet<>(builders.keySet());
                unadvertised.removeAll(commands);
                report.add("\nMissing dedicated builders: " + missing);
                report.add("\nBuilders not advertised by this server: " + unadvertised);
                Files.write(directory.resolve("coverage.md"), report);
                System.out.println("TS3 " + version.version() + " over " + protocol + ": " + commands.size()
                        + " advertised commands, " + missing.size() + " missing dedicated builders: " + missing);
                System.out.println("Report: " + directory.resolve("coverage.md"));
            }
        }
    }
}
