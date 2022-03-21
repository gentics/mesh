package com.gentics.mesh.core.db;

import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

/**
 * A transactional describes a action or a chain of actions that are meant to be executed within a transaction. The goal of this interface is to provide a
 * formal way of describing actions that are bound to be executed in a transaction. By using {@link Transactional} code can be avoided which is executed outside
 * of an existing transaction.
 * 
 * @param <T>
 */
public interface Transactional<T> {

	/**
	 * Run the transactional action using the given transaction.
	 * 
	 * @param tx
	 * @return
	 */
	T runInExistingTx(Tx tx);

	/**
	 * Run the transactional action using a newly created transaction.
	 * 
	 * @return
	 */
	T runInNewTx();

	/**
	 * Run the transaction using a map-in function. This is useful when handling mapped streams.
	 * 
	 * @param <R>
	 * @param mapper
	 * @return
	 */
	<R> Transactional<R> mapInTx(BiFunction<? super Tx, ? super T, ? extends R> mapper);

	/**
	 * Run the transactional as a flatmap operation.
	 * 
	 * @param <R>
	 * @param mapper
	 * @return
	 */
	<R> Transactional<R> flatMap(Function<? super T, Transactional<? extends R>> mapper);

	/**
	 * Run the transactional using the provided mapper.
	 * 
	 * @param <R>
	 * @param mapper
	 * @return
	 */
	default <R> Transactional<R> mapInTx(Function<T, R> mapper) {
		return mapInTx((tx, t) -> mapper.apply(t));
	}
}
