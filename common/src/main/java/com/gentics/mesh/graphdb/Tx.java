package com.gentics.mesh.graphdb;

import com.syncleus.ferma.FramedTransactionalGraph;

/**
 * A {@link Tx} is an interface for autoclosable transactions.
 */
public interface Tx extends AutoCloseable {

	/**
	 * Mark the transaction as succeeded. The autoclosable will invoke a commit when completing.
	 */
	void success();

	/**
	 * Mark the transaction as failed. The autoclosable will invoke a rollback when completing.
	 */
	void failure();

	/**
	 * Return the framed graph that is bound to the transaction.
	 * 
	 * @return
	 */
	FramedTransactionalGraph getGraph();

	/**
	 * Invoke rollback or commit when closing the autoclosable. By default a rollback will be invoked.
	 */
	@Override
	void close();

}
