package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedTransactionalGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * An abstract class that can be used to implement vendor specific graph database Trx classes.
 */
public abstract class AbstractTx extends AbstractTrxBase<FramedTransactionalGraph>implements Tx {

	protected static final Logger log = LoggerFactory.getLogger(AbstractTx.class);

	private boolean isSuccess = false;

	@Override
	public void success() {
		isSuccess = true;
	}

	@Override
	public void failure() {
		isSuccess = false;
	}

	/**
	 * Return the state of the success status flag.
	 * 
	 * @return
	 */
	protected boolean isSuccess() {
		return isSuccess;
	}

	@Override
	public void close() {
		Database.setThreadLocalGraph(getOldGraph());
		if (isSuccess()) {
			commit();
		} else {
			rollback();
		}
		// Restore the old graph that was previously swapped with the current graph
		getGraph().close();
		getGraph().shutdown();
	}

	/**
	 * Invoke a commit on the database of this transaction.
	 */
	protected void commit() {
		if (log.isDebugEnabled()) {
			log.debug("Commiting graph {" + getGraph().hashCode() + "}.");
		}
		long start = System.currentTimeMillis();
		if (getGraph() instanceof FramedTransactionalGraph) {
			((FramedTransactionalGraph) getGraph()).commit();
		}
		long duration = System.currentTimeMillis() - start;
		if (log.isDebugEnabled()) {
			log.debug("Comitting took: " + duration + " [ms]");
		}
	}

	/**
	 * Invoke a rollback on the database of this transaction.
	 */
	protected void rollback() {
		if (log.isDebugEnabled()) {
			log.debug("Invoking rollback on graph {" + getGraph().hashCode() + "}.");
		}
		if (getGraph() instanceof FramedTransactionalGraph) {
			((FramedTransactionalGraph) getGraph()).rollback();
		}
	}

}
