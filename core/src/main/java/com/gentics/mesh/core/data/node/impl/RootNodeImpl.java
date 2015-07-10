package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PARENT_NODE;

import java.util.List;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.node.ContainerNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.RootNode;

public class RootNodeImpl extends AbstractGenericNode implements RootNode {

	@Override
	public List<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public void setParentNode(ContainerNode parentNode) {
		throw new UnsupportedOperationException("The root node can't have any parent node.");
	}

	@Override
	public Node create() {
		Node node = BootstrapInitializer.getBoot().nodeRoot().create();
		node.setParentNode(this);
		return node;
	}

}
