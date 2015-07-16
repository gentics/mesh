package com.gentics.mesh.core.data;

import com.tinkerpop.blueprints.Element;

public interface MeshElement {

	String getUuid();

	void setUuid(String uuid);

	Element getElement();

}
