package com.gentics.mesh.core.data.model.node.field.impl.nesting;

import com.gentics.mesh.core.data.model.generic.MeshEdge;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.model.node.impl.MeshNodeImpl;

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
		return getProperty("field-key");
	}

	@Override
	public void setFieldKey(String key) {
		setProperty("field-key", key);
	}

	@Override
	public MeshNode getNode() {
		return inV().has(MeshNodeImpl.class).nextOrDefaultExplicit(MeshNodeImpl.class, null);
	}

}
