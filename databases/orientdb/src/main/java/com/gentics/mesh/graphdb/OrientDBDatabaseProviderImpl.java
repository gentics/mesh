package com.gentics.mesh.graphdb;

import io.vertx.core.json.JsonObject;

import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;

public class OrientDBDatabaseProviderImpl implements DatabaseServiceProvider {

	@Override
	public FramedTransactionalGraph getFramedGraph(JsonObject settings) {
		OrientTransactionalGraph memoryGraph = new OrientGraph("memory:tinkerpop");

		// Add some indices
		// memoryGraph.createKeyIndex("name", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Vertex.class);
		// memoryGraph.createKeyIndex("ferma_type", Edge.class);

		FramedTransactionalGraph fg = new DelegatingFramedTransactionalGraph<>(memoryGraph, true, false);
		return fg;
	}

}
