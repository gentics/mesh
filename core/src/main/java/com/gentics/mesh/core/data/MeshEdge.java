package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.graphdb.model.MeshElement;

public interface MeshEdge extends MeshElement {

	// Edge getEdge();

	MeshEdgeImpl getImpl();
}
