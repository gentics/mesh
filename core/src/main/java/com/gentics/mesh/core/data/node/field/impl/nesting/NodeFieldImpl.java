package com.gentics.mesh.core.data.node.field.impl.nesting;

import com.gentics.mesh.core.data.generic.MeshEdge;
import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.node.impl.MeshNodeImpl;

public class NodeFieldImpl extends MeshEdge implements NodeField {

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
