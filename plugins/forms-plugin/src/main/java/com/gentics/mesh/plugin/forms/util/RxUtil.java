package com.gentics.mesh.plugin.forms.util;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

public final class RxUtil {

	private static final Logger log = LoggerFactory.getLogger(RxUtil.class);

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
			return emptyMaybe();
		} else {
			return Maybe.just(item);
		}
	}

	/**
	 * Creates a maybe from a callable, if the given predicate succeeds.
	 * @param predicate the switch predicate
	 * @param provider the value provider
	 * @param <T>
	 * @return The provided item wrapped in a {@link Maybe#just(Object)} or {@link Maybe#empty()} if the predicate gives out false.
	 */
	public static <T> Maybe<T> fromCallableIf(Supplier<Boolean> predicate, Supplier<T> provider) {
		if (predicate.get()) {
			return Maybe.just(provider.get());
		} else {
			return emptyMaybe();
		}
	}

	/**
	 * A wrapper around Maybe.empty() with the debugging possibilities.
	 * 
	 * @param <T>
	 * @return
	 */
	public static <T> Maybe<T> emptyMaybe() {
		if (log.isDebugEnabled()) {
			log.debug("Empty Maybe at:" + Arrays.stream(Thread.currentThread().getStackTrace())
				.map(StackTraceElement::toString)
				.collect(Collectors.joining("\n\t")));
		}
		return Maybe.empty();
	}
}
