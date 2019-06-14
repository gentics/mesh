package com.gentics.mesh.madl.tp3.mock;

import com.tinkerpop.blueprints.Vertex;

public interface GraphTraversalSource {

	GraphTraversal<Vertex, Vertex> V(final Object... vertexIds);

}
