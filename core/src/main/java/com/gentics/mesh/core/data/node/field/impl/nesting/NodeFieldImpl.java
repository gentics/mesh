package com.gentics.mesh.core.data.node.field.impl.nesting;

import com.gentics.mesh.core.data.generic.MeshEdge;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;

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
	public Node getNode() {
		return inV().has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
	}

}
