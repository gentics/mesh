package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Util {
	private static final Logger log = LoggerFactory.getLogger(Util.class);

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

	public static <T> T requireType(Class<T> clazz, Object obj) {
		if (clazz.isAssignableFrom(obj.getClass())) {
			return (T) obj;
		} else {
			throw new RuntimeException(String.format("Unexpected type. Required {%s}, but got {%s}", clazz.getSimpleName(), obj.getClass().getSimpleName()));
		}
	}

	public static <T> Optional<T> warningOptional(String warningMessage, T value) {
		Optional<T> opt = Optional.ofNullable(value);
		if (!opt.isPresent()) {
			log.warn(warningMessage);
		}
		return opt;
	}

	public static <T> Stream<T> toStream(Optional<T> opt) {
		return opt.map(Stream::of).orElse(Stream.empty());
	}

	public static <T> Collector<T, ?, Flowable<T>> toFlowable() {
		return Collectors.collectingAndThen(Collectors.toList(), Flowable::fromIterable);
	}

	public static <T> Flowable<T> toFlowable(Optional<T> opt) {
		return opt.isPresent()
			? Flowable.just(opt.get())
			: Flowable.empty();
	}

	public static Flowable<SearchRequest> toRequests(Map<String, IndexInfo> map) {
		return Flowable.fromIterable(map.values())
			.map(index -> provider -> provider.createIndex(index));
	}
}
