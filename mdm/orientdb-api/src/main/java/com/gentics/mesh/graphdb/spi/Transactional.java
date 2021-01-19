package com.gentics.mesh.graphdb.spi;

import com.gentics.mesh.core.db.Tx;

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
	T runInExistingTx(Tx tx);

	T runInNewTx();

	// <R> Transactional<R> map(Function<T, R> mapper);
	<R> Transactional<R> mapInTx(BiFunction<? super Tx, ? super T, ? extends R> mapper);

	<R> Transactional<R> flatMap(Function<? super T, Transactional<? extends R>> mapper);

	default <R> Transactional<R> mapInTx(Function<T, R> mapper) {
		return mapInTx((tx, t) -> mapper.apply(t));
	}
}
