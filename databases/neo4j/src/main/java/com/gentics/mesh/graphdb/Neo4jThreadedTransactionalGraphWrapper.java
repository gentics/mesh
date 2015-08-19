package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;

public class Neo4jThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {

	private Neo4j2Graph graph;

	public Neo4jThreadedTransactionalGraphWrapper(Neo4j2Graph graph) {
		this.graph = graph;
	}

	@Override
	public TransactionalGraph newTransaction() {
		return getGraph();
	}

	@Override
	public TransactionalGraph getGraph() {
		return graph;
	}

}
