package com.gentics.mesh.graphdb;

import com.syncleus.ferma.FramedGraph;

/**
 * A {@link NoTx} is a interface for autoclosable transactions that do not need to be committed. (NoTrx is commonly used for orientdb in which a transaction is
 * started which will directly affect the modified elements.)
 *
 */
public interface NoTx extends AutoCloseable {

	/**
	 * Close the transaction.
	 */
	void close();

	/**
	 * Return the framedgraph that is currently bound to the transaction.
	 * 
	 * @return
	 */
	FramedGraph getGraph();
}
