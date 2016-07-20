package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.ferma.DelegatingFramedOrientGraph;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OrientDBNoTrx extends AbstractNoTrx {

	private static final Logger log = LoggerFactory.getLogger(OrientDBNoTrx.class);

	OrientGraphNoTx noTx = null;

	public OrientDBNoTrx(OrientGraphFactory factory, TypeResolver resolver) {
		this.noTx = factory.getNoTx();
		FramedGraph graph = new DelegatingFramedOrientGraph(noTx, resolver);
		init(graph);
	}

	@Override
	public void close() {
		if (!noTx.isClosed()) {
			noTx.shutdown();
			Database.setThreadLocalGraph(getOldGraph());
		}
	}
}
