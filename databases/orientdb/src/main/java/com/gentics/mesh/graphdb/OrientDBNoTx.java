package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.ferma.DelegatingFramedOrientGraph;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class OrientDBNoTx extends AbstractNoTx<FramedGraph> {

	OrientGraphNoTx noTx = null;

	public OrientDBNoTx(OrientGraphFactory factory, TypeResolver resolver) {
		if(Database.threadLocalGraph.get()!=null) {
			throw new RuntimeException("Warning - nested noTx detected!");
		}
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
