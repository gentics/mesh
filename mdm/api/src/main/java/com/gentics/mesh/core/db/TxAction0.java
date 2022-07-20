package com.gentics.mesh.core.db;

@FunctionalInterface
public interface TxAction0 extends Runnable {

    void handle() throws Exception;

    @Override
    default void run() {
    	try {
			handle();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
}
