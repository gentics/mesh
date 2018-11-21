package com.gentics.diktyo.db;

import java.util.function.Function;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import com.gentics.diktyo.index.IndexManager;
import com.gentics.diktyo.tx.Tx;
import com.gentics.diktyo.tx.TxAction;
import com.gentics.diktyo.tx.TxAction0;
import com.gentics.diktyo.tx.TxAction1;
import com.gentics.diktyo.tx.TxAction2;
import com.gentics.diktyo.wrapper.traversal.WrappedTraversal;

public interface Database extends AutoCloseable {

	/**
	 * Open the db.
	 * 
	 * @param name
	 * @param type
	 */
	void open(String name, DatabaseType type);

	/**
	 * Close the db.
	 */
	void close();

	/**
	 * Check whether the db is open.
	 * 
	 * @return
	 */
	boolean isOpen();

	/**
	 * Return the index management.
	 * 
	 * @return
	 */
	IndexManager index();

	/**
	 * Return a new transaction.
	 * 
	 * @return
	 */
	Tx tx();

	/**
	 * Use a new transaction and execute the given handler inside it.
	 * 
	 * @param txHandler
	 * @return
	 */
	<T> T tx(TxAction<T> txHandler);

	/**
	 * Use a new transaction and execute the given handler inside it.
	 * 
	 * @param txHandler
	 */
	default void tx(TxAction0 txHandler) {
		tx((tx) -> {
			txHandler.handle();
		});
	}

	/**
	 * Use a new transaction and execute the given handler inside it.
	 * 
	 * @param txHandler
	 * @return
	 */
	default <T> T tx(TxAction1<T> txHandler) {
		return tx((tx) -> {
			return txHandler.handle();
		});
	}

	/**
	 * Use a new transaction and execute the given handler inside it.
	 * 
	 * @param txHandler
	 */
	default void tx(TxAction2 txHandler) {
		tx((tx) -> {
			txHandler.handle(tx);
			return null;
		});
	}

	/**
	 * Create a new wrapped vertex and return it.
	 * 
	 * @param clazzOfR
	 * @return
	 */
	<T> T createVertex(Class<T> clazzOfR);

	<T extends WrappedTraversal<?>> T traverse(Function<GraphTraversalSource, GraphTraversal<?, ?>> traverser);

}
