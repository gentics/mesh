package com.gentics.mesh.graphdb;

import com.gentics.mesh.graphdb.ThreadedTransactionalGraphWrapper;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

public class TinkerGraphThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {

	public TinkerGraphThreadedTransactionalGraphWrapper(TinkerGraph graph) {
		this.graph = (TransactionalGraph) graph;
	}

	@Override
	public TransactionalGraph newTransaction() {
		return graph;
	}

}
