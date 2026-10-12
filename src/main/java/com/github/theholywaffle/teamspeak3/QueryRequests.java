package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.CommandFutures;
import com.github.theholywaffle.teamspeak3.api.exception.TS3Exception;
import com.github.theholywaffle.teamspeak3.api.wrapper.Wrapper;
import com.github.theholywaffle.teamspeak3.commands.Command;
import com.github.theholywaffle.teamspeak3.commands.response.DefaultArrayResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The single command execution path shared by every asynchronous domain API. */
final class QueryRequests {

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
		return response(command, response -> response.getResponses().isEmpty()
			? Optional.empty() : Optional.ofNullable(fn.apply(response.getFirstResponse())));
	}

	CompletableFuture<Optional<Integer>> optionalInteger(Command command, String property) {
		return optional(command, row -> parse(command, property, required(command, row, property)));
	}

	CompletableFuture<String> string(Command command, String property) {
		return one(command, row -> required(command, row, property));
	}

	CompletableFuture<Integer> integer(Command command, String property) {
		return one(command, row -> parse(command, property, required(command, row, property)));
	}

	CompletableFuture<List<Integer>> integers(Command command, String property) {
		return rows(command, row -> parse(command, property, required(command, row, property)));
	}

	CompletableFuture<Integer> createdId(Command command, String property) {
		return response(command, response -> {
			String value = response.getResponses().isEmpty() ? null : response.getFirstResponse().getMap().get(property);
			String message = command.getName() + " response requires a positive " + property;
			int id;
			try {
				id = Integer.parseInt(value);
			} catch (NumberFormatException malformed) {
				throw new TS3Exception(message, malformed);
			}
			if (id <= 0) throw new TS3Exception(message);
			return id;
		});
	}

	private static Wrapper firstRow(Command command, DefaultArrayResponse response) {
		if (response.getResponses().isEmpty()) throw new TS3Exception(command.getName() + " returned no result");
		return response.getFirstResponse();
	}

	private static String required(Command command, Wrapper row, String property) {
		String value = row.getMap().get(property);
		if (value == null) throw new TS3Exception(command.getName() + " response lacks " + property);
		return value;
	}

	private static int parse(Command command, String property, String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException failure) {
			throw new TS3Exception(command.getName() + " response requires an integer " + property, failure);
		}
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
