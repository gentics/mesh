package com.gentics.mesh.graphdb;

import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph;

public class BlueprintTransaction implements AutoCloseable {

	private TransactionalGraph currentGraph;
	private boolean isSuccess = false;

	public BlueprintTransaction(FramedThreadedTransactionalGraph graph) {
		if (graph instanceof DelegatingFramedThreadedTransactionalGraph) {
			DelegatingFramedThreadedTransactionalGraph delegatingGraph = (DelegatingFramedThreadedTransactionalGraph) graph;
			this.currentGraph = delegatingGraph.newTransaction();
		} else {
			throw new RuntimeException("Graph implementation not supported. Type: {" + graph.getClass().getName() + "}");
		}
	}

	public void success() {
		isSuccess = true;
	}

	public void failure() {
		isSuccess = false;
	}

	public FramedTransactionalGraph getGraph() {
		return new DelegatingFramedTransactionalGraph<>(currentGraph, true, false);
	}

	@Override
	public void close() {
		if (isSuccess) {
			currentGraph.commit();
		} else {
			currentGraph.rollback();
		}
	}
}
