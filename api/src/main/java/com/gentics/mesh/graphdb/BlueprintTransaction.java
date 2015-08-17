package com.gentics.mesh.graphdb;

import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph;

public class BlueprintTransaction implements AutoCloseable {

	private TransactionalGraph currentGraph;
	private boolean isSuccess = false;
	private ResettableGraph wrapper;
	private TransactionalGraph oldGraph;

	public BlueprintTransaction(FramedThreadedTransactionalGraph graph) {
		if (graph instanceof DelegatingFramedThreadedTransactionalGraph) {
			DelegatingFramedThreadedTransactionalGraph delegatingGraph = (DelegatingFramedThreadedTransactionalGraph) graph;

			if (delegatingGraph.getBaseGraph() instanceof ResettableGraph) {
				wrapper = (ResettableGraph) delegatingGraph.getBaseGraph();
				// Get the old graph from the wrapper - this can be null when we use the autocloseable is a different thread.
				this.oldGraph = wrapper.getGraph();
				// Create a new graph / transaction for our autoclosable and reset the old graph in the close method.
				this.currentGraph = delegatingGraph.newTransaction();
				wrapper.setGraph(this.currentGraph);
			} else {
				this.currentGraph = delegatingGraph.newTransaction();
			}

		//	this.currentGraph = delegatingGraph.newTransaction();
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
		if (wrapper != null && oldGraph != null) {
			wrapper.setGraph(oldGraph);
		}

	}
}
