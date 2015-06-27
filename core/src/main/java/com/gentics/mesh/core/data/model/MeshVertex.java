package com.gentics.mesh.core.data.model;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

public interface MeshVertex {

	String getUuid();

	void setUuid(String uuid);

	Vertex getVertex();

	Element getElement();
	

}
