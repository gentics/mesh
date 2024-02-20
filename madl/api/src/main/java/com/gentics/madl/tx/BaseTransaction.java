package com.gentics.madl.tx;

import java.io.IOException;
import java.util.function.Function;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Element;

import com.gentics.madl.traversal.RawTraversalResult;

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
	void close() throws IOException;

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
