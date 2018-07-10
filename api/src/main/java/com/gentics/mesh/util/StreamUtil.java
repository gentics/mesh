package com.gentics.mesh.util;

import java.util.stream.Stream;

public final class StreamUtil {

	/**
	 * Convert a stream to an iterable.
	 * 
	 * @param stream
	 * @return
	 */
	public static <T> Iterable<T> toIterable(Stream<T> stream) {
		return (Iterable<T>) () -> stream.iterator();
	}
}
