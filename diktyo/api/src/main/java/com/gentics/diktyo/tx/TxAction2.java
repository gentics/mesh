package com.gentics.diktyo.tx;

@FunctionalInterface
public interface TxAction2 {

	void handle(Tx tx) throws Exception;

}
