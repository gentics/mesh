package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {

	private OrientGraphFactory factory;

	private TransactionalGraph graph;

	public OrientThreadedTransactionalGraphWrapper(OrientGraphFactory factory) {
		this.factory = factory;
	}

	public void setFactory(OrientGraphFactory factory) {
		this.factory = factory;
	}

	@Override
	public TransactionalGraph newTransaction() {
		OrientGraph newGraph = factory.getTx();
		newGraph.getRawGraph().activateOnCurrentThread();
		return newGraph;
	}

	@Override
	public TransactionalGraph getGraph() {
		TransactionalGraph graph = null;
		if (this.graph != null) {
			graph = this.graph;
		} else {
			graph = newTransaction();
			this.graph = graph;
		}
		return graph;
	}

}
