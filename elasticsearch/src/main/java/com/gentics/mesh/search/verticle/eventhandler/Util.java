package com.gentics.mesh.search.verticle.eventhandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

public final class Util {
	private Util() {
	}

	public static <V, K> Collector<V, ?, Map<K, V>> toListWithMultipleKeys(Function<V, Collection<K>> keyMapper) {
		return Collector.of(HashMap::new,
			(map, item) -> keyMapper.apply(item).stream().forEach(key -> map.put(key, item)),
			(m1, m2) -> {
				m1.putAll(m2);
				return m1;
		});
	}
}
