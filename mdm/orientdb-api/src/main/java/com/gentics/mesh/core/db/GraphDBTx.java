package com.gentics.mesh.core.db;

import com.syncleus.ferma.FramedTransactionalGraph;

/**
 * A GraphDB-specific extension to {@link Tx}
 * 
 * @author plyhun
 *
 */
public interface GraphDBTx extends CommonTx, BaseTransaction {

	/**
	 * Return the framed graph that is bound to the transaction.
	 *
	 * @return Graph which is bound to the transaction.
	 */
	FramedTransactionalGraph getGraph();

	/**
	 * Add new isolated vertex to the graph.
	 * 
	 * @param <T>
	 *            The type used to frame the element.
	 * @param kind
	 *            The kind of the vertex
	 * @return The framed vertex
	 * 
	 */
	default <T> T addVertex(Class<T> kind) {
		return getGraph().addFramedVertex(kind);
	}
	
	/**
	 * Return the current active graph. A transaction should be the only place where this threadlocal is updated.
	 * 
	 * @return Currently active transaction
	 */
	static GraphDBTx getGraphTx() {
		return (GraphDBTx) Tx.get();
	}
}
