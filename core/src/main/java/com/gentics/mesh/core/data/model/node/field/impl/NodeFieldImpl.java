package com.gentics.mesh.core.data.model.node.field.impl;

import com.gentics.mesh.core.data.model.generic.MeshEdge;
import com.gentics.mesh.core.data.model.node.field.NodeField;

public class NodeFieldImpl extends MeshEdge implements NodeField {

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
	
	@Override
	public String getFieldKey() {
		return null;
	}

}
