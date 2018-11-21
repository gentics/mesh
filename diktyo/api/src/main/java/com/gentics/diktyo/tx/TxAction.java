package com.gentics.diktyo.tx;

@FunctionalInterface
public interface TxAction<T> {

	T handle(Tx tx) throws Exception;

}
