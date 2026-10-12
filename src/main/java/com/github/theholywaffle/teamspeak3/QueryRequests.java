package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.exception.TS3InvalidValueException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The single command execution path shared by every asynchronous domain API. */
final class QueryRequests {

	static final int INVALID_CLIENT_ID = 512;
	static final int INVALID_CHANNEL_ID = 768;
	static final int INVALID_SERVER_ID = 1024;
	static final int FILE_EXISTS = 2050;

	private final TS3Query query;
	private final CommandQueue queue;

	QueryRequests(TS3Query query, CommandQueue queue) {
		this.query = query;
		this.queue = queue;
	}

	TS3Query query() { return query; }

	CommandQueue queue() { return queue; }

	<T> CompletableFuture<T> response(Command command, Function<DefaultArrayResponse, T> fn) {
		CompletableFuture<T> result = CommandFutures.map(command.getFuture(), fn);
		queue.enqueueCommand(command);
		return result;
	}

	CompletableFuture<Void> run(Command command) {
		return response(command, ignored -> null);
	}

	<T> CompletableFuture<List<T>> rows(Command command, Function<Wrapper, T> fn) {
		return response(command, response -> response.getResponses().stream().map(fn).toList());
	}


	<T> CompletableFuture<List<T>> models(Command command, Function<Map<String, String>, T> fn) {
		return rows(command, row -> fn.apply(row.getMap()));
	}

	/** An empty result is a failure: callers asked for an entity the server must return. */
	<T> CompletableFuture<T> one(Command command, Function<Wrapper, T> fn) {
		return response(command, response -> fn.apply(firstRow(command, response)));
	}


	<T> CompletableFuture<T> model(Command command, Function<Map<String, String>, T> fn) {
		return one(command, row -> fn.apply(row.getMap()));
	}

	/** An empty result (including error 1281) means absent. */
	<T> CompletableFuture<Optional<T>> optional(Command command, Function<Wrapper, T> fn) {
		return response(command, response -> response.getFirstResponse().map(fn));
	}

	CompletableFuture<Optional<Integer>> optionalInteger(Command command, String property) {
		return optional(command, row -> row.requireInt(property));
	}

	CompletableFuture<String> string(Command command, String property) {
		return one(command, row -> row.require(property));
	}

	CompletableFuture<Integer> integer(Command command, String property) {
		return one(command, row -> row.requireInt(property));
	}

	CompletableFuture<List<Integer>> integers(Command command, String property) {
		return rows(command, row -> row.requireInt(property));
	}

	CompletableFuture<Integer> createdId(Command command, String property) {
		return one(command, row -> {
			int id = row.requireInt(property);
			if (id <= 0) throw new TS3InvalidValueException(property, Integer.toString(id), "a positive ID", null);
			return id;
		});
	}

	private static Wrapper firstRow(Command command, DefaultArrayResponse response) {
		return response.getFirstResponse().orElseThrow(() -> new TS3Exception(command.getName() + " returned no result"));
	}

	/** Looks up the entity with a found ID; an absent ID stays absent. */
	static <K, T> CompletableFuture<Optional<T>> thenLookup(CompletableFuture<Optional<K>> id,
	                                                        Function<K, CompletableFuture<T>> lookup) {
		return CommandFutures.compose(id, found -> found.isEmpty()
			? CompletableFuture.completedFuture(Optional.empty())
			: CommandFutures.map(lookup.apply(found.get()), Optional::of));
	}

	static <T> Optional<T> firstNamed(List<T> items, Function<T, String> name, String expected, boolean ignoreCase) {
		String wanted = ignoreCase ? expected.toLowerCase(Locale.ROOT) : expected;
		return items.stream().filter(item -> {
			String actual = name.apply(item);
			return (ignoreCase ? actual.toLowerCase(Locale.ROOT) : actual).equals(wanted);
		}).findFirst();
	}

	/** Replaces one specific server error with a successful value, keeping request cancellation linked. */
	static <T> CompletableFuture<T> recover(CompletableFuture<T> request, int errorId, T replacement) {
		return CommandFutures.link(request.handle((value, failure) -> {
			if (failure == null) return value;
			Throwable cause = CommandFutures.unwrap(failure);
			try { throw cause; }
			catch (TS3Exception error) {
				if (error.matchesQueryError(errorId)) return replacement;
				throw error;
			} catch (Throwable other) { throw new CompletionException(other); }
		}), request);
	}

	static CompletableFuture<Void> discard(CompletableFuture<?> request) {
		return CommandFutures.map(request, ignored -> null);
	}

	static <K, V> CompletableFuture<List<V>> findByKey(CompletableFuture<List<K>> keysFuture, CompletableFuture<List<V>> valuesFuture,
	                                                  Function<? super V, ? extends K> keyMapper) {
		return CommandFutures.link(CommandFutures.compose(keysFuture, keys -> CommandFutures.map(valuesFuture, values -> {
			Map<K, V> valueMap = values.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (l, r) -> l));
			List<V> foundValues = new ArrayList<>(keys.size());
			for (K key : keys) {
				if (key == null) continue;
				V value = valueMap.get(key);
				if (value != null) foundValues.add(value);
			}
			return List.copyOf(foundValues);
		})), keysFuture, valuesFuture);
	}
}
