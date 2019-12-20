package com.gentics.mesh.graphdb.spi;

import com.gentics.madl.tx.Tx;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
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
	Single<T> runInAsyncTx();

	/**
	 * When subscribed, creates a new transaction in a worker thread and runs the operation.
	 * @return
	 */
	Maybe<T> runInNullableAsyncTx();

	/**
	 * Creates a new transaction in a worker thread and caches the result. Returns the cached result when subscribed.
	 * @return
	 */
	Single<T> runInAsyncTxImmediately();

	/**
	 * Creates a new transaction in a worker thread and caches the result. Returns the cached result when subscribed.
	 * @return
	 */
	Maybe<T> runInNullableAsyncTxImmediately();

//	<R> Transactional<R> map(Function<T, R> mapper);
	<R> Transactional<R> mapInTx(BiFunction<Tx, T, R> mapper);
	<R> Transactional<R> flatMap(Function<T, Transactional<R>> mapper);
}
