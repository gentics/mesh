package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBNoTrx extends AbstractNoTrx implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(OrientDBNoTrx.class);

	public OrientDBNoTrx(FramedGraph graph) {
		init(graph);
	}

	@Override
	public void close() {
		getGraph().close();
		Database.setThreadLocalGraph(getOldGraph());
	}
}
