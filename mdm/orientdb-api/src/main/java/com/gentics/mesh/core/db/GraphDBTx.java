package com.gentics.mesh.core.db;

import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.madl.tp3.mock.Element;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.tp3.mock.GraphTraversalSource;
import com.syncleus.ferma.FramedTransactionalGraph;

/**
 * A GraphDB-specific extension to {@link Tx}
 * 
 * @author plyhun
 *
 */
public interface GraphDBTx extends CommonTx, GraphDBBaseTransaction {

	/**
	 * Return the framed graph that is bound to the transaction.
	 *
	 * @return Graph which is bound to the transaction.
	 */
	FramedTransactionalGraph getGraph();

	/**
	 * Return a framed / wrapped traversal.
	 * 
	 * @param traverser
	 * @return
	 */
	<T extends RawTraversalResult<?>> T traversal(Function<GraphTraversalSource, GraphTraversal<?, ?>> traverser);

	/**
	 * Return a raw traversal.
	 * 
	 * @return
	 */
	GraphTraversalSource rawTraverse();

	/**
	 * Create a new wrapped vertex and return it.
	 * 
	 * @param clazzOfR
	 * @return
	 */
	<T> T createVertex(Class<T> clazzOfR);

	/**
	 * Load the element with the given id.
	 * 
	 * @param id
	 * @return
	 */
	<E extends Element> E getElement(Object id);

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

	Binaries binaries();
}
