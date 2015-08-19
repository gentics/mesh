package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.ThreadedTransactionalGraphWrapper;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class TinkerGraphThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {

	private TinkerGraph graph;

	public TinkerGraphThreadedTransactionalGraphWrapper(TinkerGraph graph) {
		this.graph = graph;
	}

	@Override
	public TransactionalGraph newTransaction() {
		return (TransactionalGraph) graph;
	}

	@Override
	public TransactionalGraph getGraph() {
		return (TransactionalGraph) graph;
	}

}
