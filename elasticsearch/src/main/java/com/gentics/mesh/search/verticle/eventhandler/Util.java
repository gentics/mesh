package com.gentics.mesh.search.verticle.eventhandler;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.CreateIndexRequest;
import com.gentics.mesh.core.data.search.request.DropIndexRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.common.ContainerType;

import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Various static utility functions
 */
public final class Util {
	private static final Logger log = LoggerFactory.getLogger(Util.class);
	public static final Object dummyObject = new Object();

	private Util() {
	}

	/**
	 * Collects items to a map with multiple values.
	 * The item will be stored in every key returned by the keyMapper parameter.
	 * @param keyMapper
	 * @param <K>
	 * @param <V>
	 * @return
	 */
	public static <K, V> Collector<V, ?, Map<K, List<V>>> toMultiMap(Function<V, Collection<K>> keyMapper) {
		return Collector.of(
			HashMap::new,
			(map, item) -> keyMapper.apply(item).forEach(key -> {
				List<V> list = map.computeIfAbsent(key, k -> new ArrayList<>());
				list.add(item);
			}),
			(m1, m2) -> {
				m1.putAll(m2);
				return m1;
			}
		);
	}

	/**
	 * Tests if an object is an instance of a certain type. Throws an error if the type is not met.
	 * @param clazz
	 * @param obj
	 * @param <T>
	 * @return
	 */
	public static <T> T requireType(Class<T> clazz, Object obj) {
		if (clazz.isAssignableFrom(obj.getClass())) {
			return (T) obj;
		} else {
			throw new RuntimeException(String.format("Unexpected type. Required {%s}, but got {%s}", clazz.getSimpleName(), obj.getClass().getSimpleName()));
		}
	}

	/**
	 * Creates a stream of {@link ContainerType} DRAFT and PUBLISHED.
	 * @return
	 */
	public static Stream<ContainerType> latestVersionTypes() {
		return Stream.of(ContainerType.DRAFT, ContainerType.PUBLISHED);
	}

	/**
	 * Creates an optional of a nullable value and logs a warning if the value is null.
	 * @param warningMessage
	 * @param value
	 * @param <T>
	 * @return
	 */
	public static <T> Optional<T> warningOptional(String warningMessage, T value) {
		Optional<T> opt = Optional.ofNullable(value);
		if (!opt.isPresent()) {
			log.warn(warningMessage);
		}
		return opt;
	}

	/**
	 * Concatenates streams.
	 * @param streams
	 * @param <T>
	 * @return
	 */
	public static <T> Stream<T> concat(Stream<T>... streams) {
		return Stream.of(streams).flatMap(Function.identity());
	}

	/**
	 * Collects all values of a stream to a list and then returns a flowable of that list.
	 * @param <T>
	 * @return
	 */
	public static <T> Collector<T, ?, Flowable<T>> toFlowable() {
		return Collectors.collectingAndThen(Collectors.toList(), Flowable::fromIterable);
	}

	/**
	 * If a value is present, returns a {@link Flowable#just(Object)} containing only that value,
	 * otherwise returns a {@link Flowable#empty()}
	 * @param opt
	 * @param <T>
	 * @return
	 */
	public static <T> Flowable<T> toFlowable(Optional<T> opt) {
		return opt.map(Flowable::just).orElse(Flowable.empty());
	}

	/**
	 * Turns the {@link IndexInfo} values into a {@link Flowable} of {@link CreateIndexRequest}
	 * @param map
	 * @return
	 */
	public static Flowable<SearchRequest> toRequests(Map<String, Optional<IndexInfo>> map) {
		return Flowable.fromIterable(map.entrySet())
			.map(entry -> entry.getValue().map(CreateIndexRequest::new)
					.map(SearchRequest.class::cast)
					.orElseGet(() -> new DropIndexRequest(entry.getKey())));
	}

	/**
	 * Logs known elasticsearch errors in a more user friendly way. If the exception is unknown, the {@code otherwise}
	 * argument will be executed.
	 * @param error
	 * @param otherwise
	 */
	public static void logElasticSearchError(Throwable error, Runnable otherwise) {
		if (error instanceof ConnectException) {
			log.error("Could not connect to Elasticsearch. Maybe it is still starting?");
		} else {
			otherwise.run();
		}
	}

	/**
	 * Runs an action if the action is not currently running by another thread. Otherwise the action is skipped.
	 * @param lock
	 * @param action
	 */
	public static void skipIfMultipleThreads(ReentrantLock lock, Runnable action) {
		boolean locked = lock.tryLock();
		if (!locked) {
			log.trace("Skipping action");
			return;
		} else {
			log.trace("Acquired lock");
			try {
				action.run();
			} finally {
				lock.unlock();
			}
		}
	}
}
