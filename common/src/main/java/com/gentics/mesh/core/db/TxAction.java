package com.gentics.mesh.core.db;

@FunctionalInterface
public interface TxAction<T> {

	T handle(Tx tx) throws Exception;

}
