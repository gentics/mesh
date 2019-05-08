package com.gentics.mesh.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamUtil {
	private StreamUtil() {

	}

	public static <T> Stream<T> toStream(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	public static <T> Stream<T> ofNullable(T... elements) {
		return Arrays.stream(elements)
			.filter(Objects::nonNull);
	}

	public static <T> Predicate<T> not(Predicate<T> predicate) {
		return predicate.negate();
	}
}
