package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.FramedTransactionalGraph;

public class Neo4jNoTrx extends AbstractNoTrx {

	public Neo4jNoTrx(FramedGraph noTransaction) {
		init(noTransaction);
	}

	@Override
	public void close() {
		FramedGraph graph = getGraph();
		if (graph instanceof FramedTransactionalGraph) {
			((FramedTransactionalGraph) graph).commit();
		}

		Database.setThreadLocalGraph(getOldGraph());
	}
}
