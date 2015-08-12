package com.gentics.mesh.core.data.node.field.impl.nesting;

import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;

public class GraphNodeFieldImpl extends MeshEdgeImpl implements GraphNodeField {

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public Node getNode() {
		return inV().has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
	}

}
