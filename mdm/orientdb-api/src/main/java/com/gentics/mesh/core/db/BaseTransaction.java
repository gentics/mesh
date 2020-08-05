package com.gentics.mesh.core.db;

import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.madl.tp3.mock.Element;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.tp3.mock.GraphTraversalSource;

public interface BaseTransaction extends AutoCloseable {

	/**
	 * Commit the transaction.
	 */
	void commit();

	/**
	 * Rollback the transaction.
	 */
	void rollback();

	/**
	 * Mark the transaction as succeeded. The autoclosable will invoke a commit when completing.
	 */
	void success();

	/**
	 * Mark the transaction as failed. The autoclosable will invoke a rollback when completing.
	 */
	void failure();

	/**
	 * Invoke rollback or commit when closing the autoclosable. By default a rollback will be invoked.
	 */
	@Override
	void close();

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
	 * Return the id of the transaction.
	 * 
	 * @return
	 */
	int txId();

}
