package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NonTrx extends AbstractTrx implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(NonTrx.class);

	public NonTrx(Database database) {
		setGraph(database.startNonTransaction());
		if (log.isDebugEnabled()) {
			log.debug("Started non transaction {" + getGraph().hashCode() + "}");
		}
		setOldGraph(getThreadLocalGraph());
		setThreadLocalGraph(getGraph());
	}

	@Override
	public void close() {
		getGraph().close();
		setThreadLocalGraph(getOldGraph());
	}
}
