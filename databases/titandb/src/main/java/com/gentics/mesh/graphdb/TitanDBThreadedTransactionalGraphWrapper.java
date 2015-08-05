package com.gentics.mesh.graphdb;

import org.apache.commons.configuration.Configuration;

import com.thinkaurelius.titan.core.TitanFactory;
import com.tinkerpop.blueprints.TransactionalGraph;

public class TitanDBThreadedTransactionalGraphWrapper extends ThreadedTransactionalGraphWrapper {

	Configuration configuration;

	public TitanDBThreadedTransactionalGraphWrapper(Configuration configuration) {
		this.configuration = configuration;
		graph = TitanFactory.open(configuration);
	}

	@Override
	public TransactionalGraph newTransaction() {
		return TitanFactory.open(configuration);
	}

}
