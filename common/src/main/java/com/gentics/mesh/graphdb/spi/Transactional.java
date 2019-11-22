package com.gentics.mesh.graphdb.spi;

import com.gentics.madl.tx.Tx;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

public interface Transactional<T> {
	T runInExistingTx(Tx tx);
	T runInNewTx();
	Single<T> runInAsyncTx();
	Maybe<T> runInNullableAsyncTx();
//	<R> Transactional<R> map(Function<T, R> mapper);
	<R> Transactional<R> mapInTx(BiFunction<Tx, T, R> mapper);
	<R> Transactional<R> flatMap(Function<T, Transactional<R>> mapper);
}
