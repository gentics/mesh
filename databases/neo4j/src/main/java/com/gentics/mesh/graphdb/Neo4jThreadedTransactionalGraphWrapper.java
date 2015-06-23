package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;

public class Neo4jThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {


	public Neo4jThreadedTransactionalGraphWrapper(Neo4j2Graph graph) {
		setGraph(graph);
	}

	@Override
	public TransactionalGraph newTransaction() {
		return getGraph();
	}

}
