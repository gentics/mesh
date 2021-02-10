package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.Tx;

@FunctionalInterface
public interface TxAction<T> {

	T handle(Tx tx) throws Exception;

}
