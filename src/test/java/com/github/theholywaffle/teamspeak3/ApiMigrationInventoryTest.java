package com.github.theholywaffle.teamspeak3;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Keeps docs/api-migration.md complete and resolvable against the 2.0 API. */
class ApiMigrationInventoryTest {
	private static final Path GUIDE = Path.of("docs", "api-migration.md");
	private static final Pattern ROW = Pattern.compile("^\\| `([^`]+)` \\| (Retained|Replaced|Removed|Deferred) \\| `([^`]+)` \\|.*\\|$");
	private static final Pattern DOMAIN_CALL = Pattern.compile("(\\w+)\\(\\)\\.(\\w+)\\(.*");
	private static final Pattern ROOT_CALL = Pattern.compile("(\\w+)\\(.*");
	private static final Pattern TYPE_CALL = Pattern.compile("(\\w+)#(\\w+)\\(.*");

	@Test void everyPreviousOperationHasOneResolvableRow() throws Exception {
		List<String> lines = Files.readAllLines(GUIDE);
		Set<String> signatures = new HashSet<>();
		int rows = 0;
		for (String line : lines) {
			Matcher row = ROW.matcher(line);
			if (!row.matches()) continue;
			rows++;
			assertTrue(signatures.add(row.group(1)), "Duplicate row " + row.group(1));
			assertNotEquals("Deferred", row.group(2), "The guide states that no operation is deferred");
			assertResolves(row.group(3), row.group(1));
			if (row.group(2).equals("Retained")) assertRetained(row.group(1));
		}
		assertEquals(238, rows, "Every previous TS3Api/TS3ApiAsync signature needs exactly one row");
	}

	@Test void guideShowsTheExecutedWorkflow() throws Exception {
		String guide = Files.readString(GUIDE);
		String source = Files.readString(Path.of("src", "test", "java", "com", "github", "theholywaffle", "teamspeak3", "CommonWorkflow.java"));
		for (String form : List.of("sync", "async")) {
			String body = source.split("// workflow-" + form + "-start\n")[1].split("\t// workflow-" + form + "-end")[0];
			String unindented = Arrays.stream(body.stripTrailing().split("\n"))
				.map(line -> line.startsWith("\t") ? line.substring(1) : line).collect(Collectors.joining("\n"));
			assertTrue(guide.contains("```java\n" + unindented + "\n```"), form + " workflow in the guide must match CommonWorkflow");
		}
	}

	private static void assertResolves(String replacement, String row) {
		Matcher domain = DOMAIN_CALL.matcher(replacement);
		Matcher type = TYPE_CALL.matcher(replacement);
		Matcher root = ROOT_CALL.matcher(replacement);
		if (domain.matches()) {
			Class<?> domainType = method(TS3Api.class, domain.group(1), row).getReturnType();
			method(domainType, domain.group(2), row);
			method(TS3ApiAsync.class, domain.group(1), row);
		} else if (type.matches()) {
			assertDoesNotThrow(() -> method(Class.forName("com.github.theholywaffle.teamspeak3." + type.group(1)), type.group(2), row), row);
		} else if (root.matches()) {
			method(TS3Api.class, root.group(1), row);
			method(TS3ApiAsync.class, root.group(1), row);
		} else {
			fail("Unrecognized replacement " + replacement + " for " + row);
		}
	}

	private static void assertRetained(String signature) {
		String name = signature.substring(0, signature.indexOf('('));
		String parameters = signature.substring(signature.indexOf('(') + 1, signature.length() - 1);
		boolean found = Arrays.stream(TS3Api.class.getMethods()).anyMatch(method -> method.getName().equals(name)
			&& String.join(", ", Arrays.stream(method.getGenericParameterTypes()).map(ApiMigrationInventoryTest::simpleName).toList()).equals(parameters));
		assertTrue(found, "Retained operation must keep its signature: " + signature);
	}

	private static String simpleName(java.lang.reflect.Type type) {
		return type.getTypeName().replaceAll("[\\w.]+\\.(\\w+)", "$1");
	}

	private static Method method(Class<?> type, String name, String row) {
		return Arrays.stream(type.getMethods()).filter(method -> method.getName().equals(name)).findFirst()
			.orElseThrow(() -> new AssertionError(type.getSimpleName() + " lacks " + name + " for " + row));
	}
}
