package com.gentics.mesh.graphdb.spi;

import com.gentics.madl.tx.Tx;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;

public interface Transactional<T> {
	/**
	 * Runs the operation in an existing transaction.
	 * @param tx
	 * @return
	 */
	T runInExistingTx(Tx tx);

	/**
	 * Creates a new transaction and synchronously runs the operation.
	 * @return
	 */
	T runInNewTx();

	/**
	 * When subscribed, creates a new transaction in a worker thread and runs the operation.
	 * @return
	 */
	Maybe<T> runInNullableAsyncTx();

	/**
	 * When subscribed, creates a new transaction in a worker thread and runs the operation.
	 * @return
	 */
	default Single<T> runInAsyncTx() {
		return runInNullableAsyncTx().toSingle();
	}

	/**
	 * Creates a new transaction in a worker thread and caches the result. Returns the cached result when subscribed.
	 * @return
	 */
	default Single<T> runInAsyncTxImmediately() {
		Single<T> cache = runInAsyncTx().cache();
		cache.subscribe(ignore -> {}, err -> {});
		return cache;
	}

	/**
	 * Creates a new transaction in a worker thread and caches the result. Returns the cached result when subscribed.
	 * @return
	 */
	default Maybe<T> runInNullableAsyncTxImmediately() {
		Maybe<T> cache = runInNullableAsyncTx().cache();
		cache.subscribe(ignore -> {}, err -> {});
		return cache;
	}

	default <R> Transactional<R> map(Function<T, R> mapper) {
		return flatMap(item -> of(mapper.apply(item)));
	}

	<R> Transactional<R> flatMap(Function<T, Transactional<R>> mapper);

	/**
	 * Returns an item wrapped in an transactional.
	 * All runIn* methods do not actually start or need a transaction.
	 *
	 * @param item
	 * @param <T>
	 * @return
	 */
	static <T> Transactional<T> of(T item) {
		return new Transactional<T>() {
			@Override
			public T runInExistingTx(Tx tx) {
				return item;
			}

			@Override
			public T runInNewTx() {
				return item;
			}

			@Override
			public Maybe<T> runInNullableAsyncTx() {
				return Maybe.just(item);
			}

			@Override
			public <R> Transactional<R> flatMap(Function<T, Transactional<R>> mapper) {
				try {
					return mapper.apply(item);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
