package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.CreateIndexRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.search.impl.SearchClient;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Util {
	private static final Logger log = LoggerFactory.getLogger(Util.class);
	public static final Object dummyObject = new Object();

	private Util() {
	}

	public static <V, K> Collector<V, ?, Map<K, V>> toListWithMultipleKeys(Function<V, Collection<K>> keyMapper) {
		return Collector.of(HashMap::new,
			(map, item) -> keyMapper.apply(item).forEach(key -> map.put(key, item)),
			(m1, m2) -> {
				m1.putAll(m2);
				return m1;
		});
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

	public static <T> T requireType(Class<T> clazz, Object obj) {
		if (clazz.isAssignableFrom(obj.getClass())) {
			return (T) obj;
		} else {
			throw new RuntimeException(String.format("Unexpected type. Required {%s}, but got {%s}", clazz.getSimpleName(), obj.getClass().getSimpleName()));
		}
	}

	public static Stream<ContainerType> latestVersionTypes() {
		return Stream.of(ContainerType.DRAFT, ContainerType.PUBLISHED);
	}

	private Single<List<String>> loadResultItems() {

		return Maybe.just(Collections.singletonList(""))
			.flatMapSingleElement(bla -> Observable.just("a", "b")
			.<List<String>>collectInto(new ArrayList<>(), List::add))
			.toSingle(Collections.emptyList());
	}

	public static <T> Optional<T> warningOptional(String warningMessage, T value) {
		Optional<T> opt = Optional.ofNullable(value);
		if (!opt.isPresent()) {
			log.warn(warningMessage);
		}
		return opt;
	}

	public static <T> Stream<T> concat(Stream<T>... streams) {
		return Stream.of(streams).flatMap(Function.identity());
	}

	public static <T> Stream<T> toStream(Optional<T> opt) {
		return opt.map(Stream::of).orElse(Stream.empty());
	}

	public static <T> Collector<T, ?, Flowable<T>> toFlowable() {
		return Collectors.collectingAndThen(Collectors.toList(), Flowable::fromIterable);
	}

	public static <T> Flowable<T> toFlowable(Optional<T> opt) {
		return opt.map(Flowable::just).orElse(Flowable.empty());
	}

	public static Flowable<SearchRequest> toRequests(Map<String, IndexInfo> map) {
		return Flowable.fromIterable(map.values())
			.map(CreateIndexRequest::new);
	}
}
