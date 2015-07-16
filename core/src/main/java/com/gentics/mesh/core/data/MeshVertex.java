package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.tinkerpop.blueprints.Vertex;

public interface MeshVertex extends MeshElement {

	Vertex getVertex();

	MeshVertexImpl getImpl();
}
