package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedTransactionalGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractTrx extends AbstractTrxBase<FramedTransactionalGraph> implements Trx {

	private static final Logger log = LoggerFactory.getLogger(AbstractTrx.class);

	private boolean isSuccess = false;

	@Override
	public void success() {
		isSuccess = true;
	}

	@Override
	public void failure() {
		isSuccess = false;
	}

	private boolean isSuccess() {
		return isSuccess;
	}

	@Override
	public void close() {
		if (isSuccess()) {
			commit();
		} else {
			rollback();
		}
		// Restore the old graph that was previously swapped with the current graph 
		Database.setThreadLocalGraph(getOldGraph());
	}

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

	protected void rollback() {
		if (log.isDebugEnabled()) {
			log.debug("Invoking rollback on graph {" + getGraph().hashCode() + "}.");
		}
		if (getGraph() instanceof FramedTransactionalGraph) {
			((FramedTransactionalGraph) getGraph()).rollback();
		}
	}

}
