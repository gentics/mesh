package com.gentics.mesh.core.db;

import java.util.function.Function;

@FunctionalInterface
public interface TxAction<T> extends Function<Tx, T> {

	T handle(Tx tx) throws Exception;

	@Override
	default T apply(Tx tx) {
		try {
			return handle(tx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
