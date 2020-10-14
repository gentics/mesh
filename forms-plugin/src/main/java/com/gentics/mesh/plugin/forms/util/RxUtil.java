package com.gentics.mesh.plugin.forms.util;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

public final class RxUtil {

	/**
	 * Read the flowable and return a single buffer with the read data.
	 * 
	 * @param stream
	 * @return
	 */
	public static Single<Buffer> readEntireData(Flowable<byte[]> stream) {
		return stream.map(b -> Buffer.buffer(b)).reduce((a, b) -> a.appendBuffer(b)).toSingle();
	}

	/**
	 * Creates a maybe from a nullable object.
	 * @param item The item to be wrapped
	 * @param <T>
	 * @return The item wrapped in a {@link Maybe#just(Object)} or {@link Maybe#empty()} if the item is null.
	 */
	public static <T> Maybe<T> fromNullable(T item) {
		if (item == null) {
			return Maybe.empty();
		} else {
			return Maybe.just(item);
		}
	}
}
