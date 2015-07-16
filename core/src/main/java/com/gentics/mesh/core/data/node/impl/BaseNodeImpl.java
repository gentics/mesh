package com.gentics.mesh.core.data.node.impl;

import com.gentics.mesh.core.data.node.ContainerNode;

public class BaseNodeImpl extends NodeImpl {

	@Override
	public void setParentNode(ContainerNode parentNode) {
		throw new UnsupportedOperationException("The base node can't have any parent node.");
	}

	@Override
	public void delete() {
		throw new UnsupportedOperationException("The base node can't be deleted");
	}

}
