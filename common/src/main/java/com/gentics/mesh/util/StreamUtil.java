package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamUtil {
	private StreamUtil() {

	}

	public static <T> Stream<T> toStream(Iterator<T> iterator) {
		return toStream(() -> iterator);
	}

	public static <T> Stream<T> toStream(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	public static <T> Stream<T> ofNullable(T... elements) {
		return Arrays.stream(elements)
			.filter(Objects::nonNull);
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

	public static <T> Predicate<T> not(Predicate<T> predicate) {
		return predicate.negate();
	}
}
