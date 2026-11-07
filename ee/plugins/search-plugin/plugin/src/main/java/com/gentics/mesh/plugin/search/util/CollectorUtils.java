package com.gentics.mesh.plugin.search.util;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Utilities providing Java {@link Collector stream collectors}.
 */
public class CollectorUtils {

	/**
	 * Creates a {@link Collector} that will map the items in the stream to their positions in the stream.
	 *
	 * <p>
	 * When the items in the stream are not unique, they will be mapped to their first occurence int the stream.
	 * </p>
	 *
	 * @return A map from the items to the position they appeared in the stream
	 */
	public static <T> Collector<T, Map<T, Integer>, Map<T, Integer>> mapToIndex() {
		return new Collector<T, Map<T, Integer>, Map<T, Integer>>() {

			private int index = 0;

			@Override
			public Supplier<Map<T, Integer>> supplier() {
				return HashMap<T, Integer>::new;
			}

			@Override
			public BiConsumer<Map<T, Integer>, T> accumulator() {
				return (m, i) ->  m.put(i, index++);
			}

			@Override
			public BinaryOperator<Map<T, Integer>> combiner() {
				return (u, v) -> {
					v.putAll(u);

					return v;
				};
			}

			@Override
			public Function<Map<T, Integer>, Map<T, Integer>> finisher() {
				return Function.identity();
			}

			@Override
			public Set<Characteristics> characteristics() {
				return Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));
			}
		};
	}

	public static <U, V> BiConsumer<Map<U, V>, Map<U, V>> warnOnConflictMerger(Logger log, String fmt) {
		return (m1, m2) -> {
			m1.forEach((key, val) -> m2.merge(key, val, (u, v) -> {
				log.warn(String.format(fmt, key, u, v));
				return u;
			}));
		};
	}
}

