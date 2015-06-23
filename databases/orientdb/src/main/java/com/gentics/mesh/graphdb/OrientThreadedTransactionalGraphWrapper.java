package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {

	private OrientGraphFactory factory;

	TransactionalGraph graph;

	public OrientThreadedTransactionalGraphWrapper(OrientGraphFactory factory) {
		this.factory = factory;
		graph = factory.getTx();
	}

	@Override
	public TransactionalGraph newTransaction() {
		return factory.getTx();
	}

}
