package com.gentics.mesh.core.db;

@FunctionalInterface
public interface TxAction<T> {

	T handle(GraphDBTx tx) throws Exception;

}
