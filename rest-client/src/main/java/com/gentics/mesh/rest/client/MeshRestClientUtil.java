package com.gentics.mesh.rest.client;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.core.rest.MeshEvent;

import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public final class MeshRestClientUtil {
	private MeshRestClientUtil() {
	}


	/**
	 * Returns a predicate that tests if the event is one of the given.
	 *
	 * <p>Example:</p>
	 * <pre>
	 *     websocket.events()
	 *     	.filter(isOneOf(NODE_CREATED, NODE_UPDATED))
	 *     	.subscribe(...)
	 * </pre>
	 *
	 * @param events
	 * @return
	 */
	public static Predicate<EventbusEvent> isOneOf(MeshEvent... events) {
		Set<String> set = Stream.of(events)
			.map(MeshEvent::getAddress)
			.collect(Collectors.toSet());
		return value -> set.contains(value.getAddress());
	}

	/**
	 * If an HTTP error with the given code occurs, the given function will be called and this single will switch to
	 * the result of the function.
	 *
	 * @param code
	 * @param mapper
	 * @param <T>
	 * @return
	 */
	public static <T> SingleTransformer<T, T> onErrorCodeResumeNext(int code, Function<MeshRestClientMessageException, Single<T>> mapper) {
		return upstream -> upstream.onErrorResumeNext(err ->
			hasErrorCode(code).test(err)
			? mapper.apply((MeshRestClientMessageException) err)
			: Single.error(err)
		);
	}

	/**
	 * If an HTTP error with the given code occurs, this single will switch to given single.
	 *
	 * @param code
	 * @param other
	 * @param <T>
	 * @return
	 */
	public static <T> SingleTransformer<T, T> onErrorCodeResumeNext(int code, Single<T> other) {
		return upstream -> upstream.onErrorResumeNext(err ->
			hasErrorCode(code).test(err)
				? other
				: Single.error(err)
		);
	}

	/**
	 * A predicate to check if an error is an HTTP error with the given status code.
	 *
	 * @param code
	 * @param <T>
	 * @return
	 */
	public static <T extends Throwable> Predicate<T> hasErrorCode(int code) {
		return thr -> unpackErrorTo(thr, MeshRestClientMessageException.class)
			.map(err -> err.getStatusCode() == code)
			.orElse(false);
	}

	/**
	 * Unpacks a runtime error until a certain error class has been found.
	 * Returns an empty optional if the class could not be found or if the chain of causes is cyclic.
	 *
	 * @param err
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	private static <T> Optional<T> unpackErrorTo(Throwable err, Class<T> clazz) {
		HashSet<Object> alreadyChecked = new HashSet<>();
		while (err != null) {
			if (clazz.isAssignableFrom(err.getClass())) {
				return Optional.of((T) err);
			} else if (alreadyChecked.contains(err)) {
				return Optional.empty();
			}
			alreadyChecked.add(err);
			err = err.getCause();
		}
		return Optional.empty();
	}
}
