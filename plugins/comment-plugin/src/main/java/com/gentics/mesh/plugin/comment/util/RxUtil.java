package com.gentics.mesh.plugin.comment.util;

import com.gentics.mesh.plugin.comment.error.MeshException;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

import java.util.Optional;
import java.util.function.Supplier;

public class RxUtil {

	/**
	 * Transforms a Maybe to a single. If there is no element in the Maybe, the provided single is used instead.
	 * To be used with {@link Single#to(Function)}
	 *
	 * @param defaultSingle This will be used if the upstream Maybe is empty
	 */
	public static <T> Function<Maybe<T>, Single<T>> defaultSingle(Single<T> defaultSingle) {
		return source -> source.flatMap(Maybe::just, Maybe::error, defaultSingle::toMaybe).toSingle();
	}

	/**
	 * Transforms a Maybe to a single. If there is no element in the Maybe, the provided single is used instead.
	 * To be used with {@link Single#to(Function)}
	 *
	 * @param defaultSingle This will be used if the upstream Maybe is empty
	 */
	public static <T> Function<Maybe<T>, Single<T>> defaultSingle(Supplier<Single<T>> defaultSingle) {
		return source -> source.flatMap(Maybe::just, Maybe::error, () -> defaultSingle.get().toMaybe()).toSingle();
	}

	/**
	 * Creates a {@link Maybe} out of an optional
	 */
	public static <T> Maybe<T> fromOptional(Optional<T> source) {
		return source.map(Maybe::just).orElseGet(Maybe::empty);
	}

	/**
	 * Allows to wrap an exception into another exception
	 * @param clazz The class of the expected exception. Other exceptions will not be rethrown.
	 * @param rethrower A function that maps the original exception to a new one
	 * @param <E> Type of the error
	 * @param <T> Type of the single
	 * @return
	 */
	public static <E, T> SingleTransformer<T, T> rethrowSingle(Class<E> clazz, Function<E, ? extends Throwable> rethrower) {
		return upstream -> upstream.onErrorResumeNext(error -> {
			if (clazz.isInstance(error)) {
				return Single.error(rethrower.apply((E) error));
			} else {
				return Single.error(error);
			}
		});
	}

	/**
	 * Allows to wrap an exception into another exception
	 *
	 * @param clazz     The class of the expected exception. Other exceptions will not be rethrown.
	 * @param rethrower A function that maps the original exception to a new one
	 * @param <E>       Type of the error
	 * @return
	 */
	public static <E> CompletableTransformer rethrowCompletable(Class<E> clazz, Function<E, ? extends Throwable> rethrower) {
		return upstream -> upstream.onErrorResumeNext(error -> {
			if (clazz.isInstance(error)) {
				return Completable.error(rethrower.apply((E) error));
			} else {
				return Completable.error(error);
			}
		});
	}

	/**
	 * Wraps an error from mesh into a comment plugin exception
	 */
	public static <T> Single<T> expectMeshError(Single<T> upstream) {
		return upstream.compose(rethrowSingle(MeshRestClientMessageException.class, MeshException::new));
	}

	/**
	 * Wraps an error from mesh into a comment plugin exception
	 */
	public static Completable expectMeshError(Completable upstream) {
		return upstream.compose(rethrowCompletable(MeshRestClientMessageException.class, MeshException::new));
	}

	/**
	 * Combines {@link Single#zip(SingleSource, SingleSource, BiFunction)} with
	 * {@link Single#flatMap(Function)}.
	 */
	public static <T1, T2, R> Single<R> flatZip(SingleSource<T1> source1, SingleSource<T2> source2, BiFunction<? super T1, ? super T2, Single<R>> zipper) {
		return Single.zip(source1, source2, zipper).flatMap(x -> x);
	}

	/**
	 * Joins strings to a single string with a delimiter in between
	 * To be used with {@link io.reactivex.Observable#reduce}
	 */
	public static BiFunction<String, String, String> joining(String delimiter) {
		return (left, right) -> left + delimiter + right;
	}

	/**
	 * Tests if the source has not emitted an error
	 */
	public static Single<Boolean> hasNoError(Completable source) {
		return source
			.andThen(Single.just(true))
			.onErrorReturnItem(false);
	}

	/**
	 * Waits for the provided completable to complete and then returns the original item from the source single.
	 */
	public static <T> SingleTransformer<T, T> waitFor(Function<? super T, ? extends Completable> mapper) {
		return source -> source.flatMap(item -> mapper.apply(item).andThen(Single.just(item)));
	}
}
