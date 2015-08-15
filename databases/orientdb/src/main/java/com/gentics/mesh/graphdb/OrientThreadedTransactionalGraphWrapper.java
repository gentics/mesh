package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {

	private OrientGraphFactory factory;

	public OrientThreadedTransactionalGraphWrapper(OrientGraphFactory factory) {
		this.factory = factory;
		graph = factory.getTx();
	}

	public void setGraph(TransactionalGraph graph) {
		this.graph = graph;
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

}
