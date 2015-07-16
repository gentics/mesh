package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.tinkerpop.blueprints.Edge;

public interface MeshEdge extends MeshElement {

	// Edge getEdge();

	MeshEdgeImpl getImpl();
}
