package com.gentics.mesh.search.verticle.eventhandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

public final class Util {
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

	public static <T> Stream<T> toStream(Optional<T> opt) {
		return opt.map(Stream::of).orElse(Stream.empty());
	}
}
