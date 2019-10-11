package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamUtil {
	private StreamUtil() {

	}

	public static <T> Stream<T> toStream(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	public static <T> Stream<T> toStream(Iterator<T> iterator) {
		return toStream(() -> iterator);
	}

	/**
	 * Turns an optional into a stream.
	 * TODO Remove this with Java 9
	 * @param opt
	 * @param <T>
	 * @return
	 */
	public static <T> Stream<T> toStream(Optional<T> opt) {
		return opt.map(Stream::of).orElseGet(Stream::empty);
	}

	/**
	 * Applies the {@code nextValue} operator repeatedly to its previous result until null is returned.
	 *
	 * @param initialValue
	 * @param nextValue
	 * @param <T>
	 * @return
	 */
	public static <T> Stream<T> untilNull(Supplier<T> initialValue, UnaryOperator<T> nextValue) {
		return toStream(new Iterator<T>() {
			Supplier<T> next = lazy(initialValue);

			@Override
			public boolean hasNext() {
				return next.get() != null;
			}

			@Override
			public T next() {
				T returnedValue = next.get();
				next = lazy(() -> nextValue.apply(returnedValue));
				return returnedValue;
			}
		});
	}

	public static <T> Supplier<T> lazy(Supplier<T> supplier) {
		return new Supplier<T>() {
			T value;
			boolean hasBeenCalled;

			@Override
			public T get() {
				if (!hasBeenCalled) {
					hasBeenCalled = true;
					value = supplier.get();
				}
				return value;
			}
		};
	}

	public static <K, V> Collector<Map<K, V>, Map<K, V>, Map<K, V>> mergeMaps() {
		return Collector.of(
			HashMap::new,
			Map::putAll,
			(m1, m2) -> {
				m1.putAll(m2);
				return m1;
			}
		);
	}

	public static <T> Stream<T> ofNullable(T... elements) {
		return Arrays.stream(elements)
			.filter(Objects::nonNull);
	}

	public static <T> Predicate<T> not(Predicate<T> predicate) {
		return predicate.negate();
	}

	/**
	 * Merges streams into a single stream.
	 * @param streams
	 * @param <T>
	 * @return
	 */
	public static <T> Stream<T> mergeStreams(Stream<T>... streams) {
		return Stream.of(streams).flatMap(Function.identity());
	}
}
