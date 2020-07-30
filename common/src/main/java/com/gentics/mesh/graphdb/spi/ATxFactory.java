package com.gentics.mesh.graphdb.spi;

import com.gentics.madl.tx.TxAction0;
import com.gentics.madl.tx.TxAction1;
import com.gentics.mda.ATx;
import com.gentics.mda.ATxAction;
import com.gentics.mda.ATxAction2;

public interface ATxFactory {

	/**
	 * Return a new autoclosable transaction handler. This object should be used within a try-with-resource block.
	 *
	 * <pre>
	 * {
	 * 	&#64;code
	 * 	try(Tx tx = db.tx()) {
	 * 	  // interact with graph db here
	 *  }
	 * }
	 * </pre>
	 *
	 * @return Created transaction
	 */
	ATx tx();

	/**
	 * Execute the txHandler within the scope of a transaction and call
	 * the result handler once the transaction handler code has finished.
	 *
	 * @param txHandler
	 *            Handler that will be executed within the scope of the transaction.
	 * @return Object which was returned by the handler
	 */
	<T> T tx(ATxAction<T> txHandler);

	/**
	 * Execute the txHandler within the scope of a transaction.
	 *
	 * @param txHandler
	 *            Handler that will be executed within the scope of the transaction.
	 */
	default void tx(TxAction0 txHandler) {
		tx((tx) -> {
			txHandler.handle();
		});
	}

	/**
	 * Execute the txHandler within the scope of a transaction.
	 *
	 * @param txHandler
	 *            Handler that will be executed within the scope of the transaction.
	 * @return Result of the handler
	 */
	default <T> T tx(TxAction1<T> txHandler) {
		return tx((tx) -> {
			return txHandler.handle();
		});
	}

	/**
	 * Execute the txHandler within the scope of a transaction.
	 *
	 * @param txHandler
	 *            Handler that will be executed within the scope of the transaction.
	 */
	default void tx(ATxAction2 txHandler) {
		tx((tx) -> {
			txHandler.handle(tx);
			return null;
		});
	}


}
