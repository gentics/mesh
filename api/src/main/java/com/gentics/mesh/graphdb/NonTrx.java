package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NonTrx extends AbstractTrx implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(NonTrx.class);

	private FramedGraph oldLocalGraph;

	public NonTrx(Database database) {
		currentGraph = database.startNonTransaction();
		oldLocalGraph = localGraph.get();
		setLocalGraph(currentGraph);
	}

	@Override
	public void close() {
		currentGraph.close();
		setLocalGraph(oldLocalGraph);
	}
}
