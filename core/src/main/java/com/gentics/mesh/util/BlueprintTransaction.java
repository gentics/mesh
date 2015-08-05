package com.gentics.mesh.util;

import com.gentics.mesh.graphdb.ResettableGraph;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph;

public class BlueprintTransaction implements AutoCloseable {

	private TransactionalGraph oldGraph;
	private TransactionalGraph currentGraph;
	private ResettableGraph wrapper;
	private boolean isSuccess = false;

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
