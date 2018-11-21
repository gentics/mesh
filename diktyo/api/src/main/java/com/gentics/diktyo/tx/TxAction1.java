package com.gentics.diktyo.tx;

@FunctionalInterface
public interface TxAction1<T> {

	T handle() throws Exception;

}
