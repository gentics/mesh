package com.gentics.mesh.graphdb.spi;


import com.gentics.mesh.core.db.Tx;

import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

public interface Transactional<T> {
	T runInExistingTx(Tx tx);
	T runInNewTx();
//	<R> Transactional<R> map(Function<T, R> mapper);
	<R> Transactional<R> mapInTx(BiFunction<Tx, T, R> mapper);
	<R> Transactional<R> flatMap(Function<T, Transactional<R>> mapper);

	default <R> Transactional<R> mapInTx(Function<T, R> mapper) {
		return mapInTx((tx, t) -> mapper.apply(t));
	}
}
