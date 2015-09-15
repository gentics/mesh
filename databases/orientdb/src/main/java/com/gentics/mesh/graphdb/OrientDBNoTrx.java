package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBNoTrx extends AbstractNoTrx implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(OrientDBNoTrx.class);

	public OrientDBNoTrx(Database database) {
		init(database, database.startNonTransaction());
	}

	@Override
	public void close() {
		getGraph().close();
		Database.setThreadLocalGraph(getOldGraph());
	}
}
