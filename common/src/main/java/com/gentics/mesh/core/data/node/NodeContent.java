package com.gentics.mesh.core.data.node;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;

/**
 * Container object for handling nodes in combination with a specific known container.
 */
public class NodeContent {

	Node node;
	NodeGraphFieldContainer container;

	public NodeContent(Node node, NodeGraphFieldContainer container) {
		this.node = node;
		this.container = container;
	}

	public NodeContent(NodeGraphFieldContainer container) {
		this.container = container;
	}

	public Node getNode() {
		if (node == null && container != null) {
			node = container.getParentNode();
		}
		return node;
	}

	public NodeGraphFieldContainer getContainer() {
		return container;
	}

}
