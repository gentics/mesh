package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedTransactionalGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Trx extends AbstractTrx implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(Trx.class);

	private boolean isSuccess = false;

	public Trx(Database database) {
		// Start a new transaction and set the fields accordingly
		setGraph(database.startTransaction());
		if (log.isDebugEnabled()) {
			log.debug("Started transaction {" + getGraph().hashCode() + "}");
		}
		setOldGraph(getThreadLocalGraph());
		setThreadLocalGraph(getGraph());
	}

	public void success() {
		isSuccess = true;
	}

	public void failure() {
		isSuccess = false;
	}

	@Override
	public void close() {
		handleDebug();
		if (isSuccess) {
			commit();
		} else {
			rollback();
		}
		setThreadLocalGraph(getOldGraph());
	}

	private void commit() {
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

	private void rollback() {
		if (log.isDebugEnabled()) {
			log.debug("Invoking rollback on graph {" + getGraph().hashCode() + "}.");
		}
		if (getGraph() instanceof FramedTransactionalGraph) {
			((FramedTransactionalGraph) getGraph()).rollback();
		}
	}

}
