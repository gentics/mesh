package com.gentics.mesh.database;

import java.util.function.Consumer;

/**
 * Functional interface for actions to be performed with a HibernateTx
 */
@FunctionalInterface
public interface HibernateTxAction0 extends Consumer<HibernateTx> {
	/**
	 * Handle the action
	 * @param tx transaction
	 * @throws Exception
	 */
	void handle(HibernateTx tx) throws Exception;

	@Override
	default void accept(HibernateTx tx) {
		try {
			handle(tx);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
