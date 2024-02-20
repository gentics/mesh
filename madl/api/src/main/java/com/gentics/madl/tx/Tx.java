package com.gentics.madl.tx;

import java.util.Optional;

import com.syncleus.ferma.FramedGraph;

/**
 * A {@link Tx} is an interface for autoclosable transactions.
 */
public interface Tx extends BaseTransaction {

	/**
	 * Thread local that is used to store references to the used graph.
	 */
	static ThreadLocal<Tx> threadLocalGraph = new ThreadLocal<>();

	/**
	 * Set the nested active transaction for the current thread.
	 * 
	 * @param tx
	 *            Transaction
	 */
	static void set(Tx tx) {
		Tx.threadLocalGraph.set(tx);
	}

	/**
	 * Return the current active transaction. A transaction should be the only place where this threadlocal is updated.
	 * 
	 * @return Currently active transaction
	 */
	static Tx get() {
		return Tx.threadLocalGraph.get();
	}

	/**
	 * An optional wrapper around {@link Tx#get()}.
	 * 
	 * @return
	 */
	static Optional<Tx> maybeGet() {
		return Optional.ofNullable(get());
	}

	/**
	 * An automatic cast of a higher level TXx to its implementors.
	 * 
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default <T extends Tx> T unwrap() {
		return (T) this;
	}

	 /**
	 * Return the framed graph that is bound to the transaction.
	 *
	 * @return Graph which is bound to the transaction.
	 */
	 FramedGraph getGraph();

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

}
