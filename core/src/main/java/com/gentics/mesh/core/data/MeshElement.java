package com.gentics.mesh.core.data;

import com.tinkerpop.blueprints.Element;

public interface MeshElement {

	void setUuid(String uuid);

	String getUuid();

	Element getElement();

}
