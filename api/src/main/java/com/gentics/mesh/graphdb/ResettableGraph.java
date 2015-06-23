package com.gentics.mesh.graphdb;

import com.tinkerpop.blueprints.TransactionalGraph;

public interface ResettableGraph {

	public TransactionalGraph getGraph();
	
	public void setGraph(TransactionalGraph graph);
}
