package com.github.theholywaffle.teamspeak3.api.wrapper;

import com.github.theholywaffle.teamspeak3.api.ClientDatabaseId;
import com.github.theholywaffle.teamspeak3.api.ClientId;
import com.github.theholywaffle.teamspeak3.api.Snapshot;
import com.github.theholywaffle.teamspeak3.api.event.BaseEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Guards every received model and event against mutable state, arrays and legacy date types. */
class ModelImmutabilityTest {

	@Test void modelsExposeNoMutableStateOrLegacyTypes() throws Exception {
		var models = new ArrayList<Class<?>>(classesOf(Wrapper.class.getPackageName()));
		models.addAll(classesOf(BaseEvent.class.getPackageName()).stream().filter(BaseEvent.class::isAssignableFrom).toList());
		models.addAll(List.of(ClientId.class, ClientDatabaseId.class, Snapshot.class));
		assertTrue(models.size() > 45, "Model scan found " + models.size() + " classes");
		var violations = new ArrayList<String>();
		for (Class<?> model : models) {
			for (var field : model.getDeclaredFields()) {
				if (!Modifier.isFinal(field.getModifiers())) violations.add(model.getSimpleName() + "." + field.getName() + " is not final");
			}
			for (Method method : model.getDeclaredMethods()) {
				if (!Modifier.isPublic(method.getModifiers())) continue;
				Class<?> type = method.getReturnType();
				if (type.isArray() || Date.class.isAssignableFrom(type)) violations.add(model.getSimpleName() + "#" + method.getName() + " returns " + type.getSimpleName());
				if (method.getName().startsWith("set")) violations.add(model.getSimpleName() + "#" + method.getName() + " mutates");
			}
		}
		assertEquals(List.of(), violations);
	}

	private static List<Class<?>> classesOf(String packageName) throws IOException, URISyntaxException {
		Path classes = Path.of(Wrapper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		Path directory = classes.resolve(packageName.replace('.', '/'));
		try (Stream<Path> files = Files.list(directory)) {
			return files.map(file -> file.getFileName().toString())
				.filter(name -> name.endsWith(".class") && !name.contains("$"))
				.<Class<?>>map(name -> load(packageName + "." + name.substring(0, name.length() - ".class".length())))
				.filter(type -> !type.isInterface() && !type.isEnum())
				.toList();
		}
	}

	private static Class<?> load(String name) {
		try {
			return Class.forName(name);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}
}
