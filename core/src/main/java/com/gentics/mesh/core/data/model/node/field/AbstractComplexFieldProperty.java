package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.generic.MeshVertex;

public abstract class AbstractComplexFieldProperty extends MeshVertex {

	public String getLabel() {
		return getProperty("label");
	}

	public void setLabel(String label) {
		setProperty("label", label);
	}

	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

}
