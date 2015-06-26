package com.gentics.mesh.core.data.model.node.field;

import com.gentics.mesh.core.data.model.generic.MeshEdge;

public class NodeFieldProperty extends MeshEdge implements FieldProperty {

	@Override
	public void setFieldLabel(String label) {
		setProperty("label", label);
	}

	@Override
	public String getFieldName() {
		return getProperty("name");
	}

	@Override
	public void setFieldName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getFieldLabel() {
		return getProperty("label");
	}

}
