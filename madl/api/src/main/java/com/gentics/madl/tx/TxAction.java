package com.gentics.madl.tx;

@FunctionalInterface
public interface TxAction<T> {

	T handle(Tx tx) throws Exception;

}
