package com.gentics.mesh.core.db;

import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.core.data.db.BaseTransaction;
import com.gentics.mesh.madl.tp3.mock.Element;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.tp3.mock.GraphTraversalSource;

public interface GraphDBBaseTransaction extends BaseTransaction {

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
}
