package com.gentics.mesh.graphdb;

import org.apache.commons.configuration.Configuration;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.TransactionalGraph;

public class TitanDBThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {

	private Configuration configuration;
	private TitanGraph graph;

	public TitanDBThreadedTransactionalGraphWrapper(Configuration configuration) {
		this.configuration = configuration;
		graph = TitanFactory.open(configuration);
	}

	@Override
	public TransactionalGraph newTransaction() {
		return TitanFactory.open(configuration);
	}

	@Override
	public TransactionalGraph getGraph() {
		return graph;
	}

}
