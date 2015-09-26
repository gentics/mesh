package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.DelegatingFramedGraph;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBNoTrx extends AbstractNoTrx implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(OrientDBNoTrx.class);

	public OrientDBNoTrx(OrientGraphFactory factory) {
		if (Database.getThreadLocalGraph() instanceof DelegatingFramedGraph) {
			init(Database.getThreadLocalGraph());
		} else {
			FramedGraph graph = new DelegatingFramedGraph<>(factory.getNoTx(), true, false);
			init(graph);
		}
	}

	@Override
	public void close() {
		getGraph().close();
		Database.setThreadLocalGraph(getOldGraph());
	}
}
