package com.gentics.mesh.hibernate.data.domain.misc;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.NodeFieldContainerEdge;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.hibernate.data.domain.HibBranchNodeParent;

/**
 * The most fulfilling node data, collected in a single data object.
 * 
 * @author plyhun
 *
 */
public class NodeData {

	private final Node node;
	private final HibBranchNodeParent parentEdge;
	private final NodeFieldContainerEdge container;
	private final NodeFieldContainer content;

	public NodeData(Node node, HibBranchNodeParent parentEdge, NodeFieldContainerEdge container, NodeFieldContainer content) {
		super();
		this.node = node;
		this.parentEdge = parentEdge;
		this.container = container;
		this.content = content;
	}

	public Node getNode() {
		return node;
	}

	public HibBranchNodeParent getParentEdge() {
		return parentEdge;
	}

	public NodeFieldContainerEdge getContainer() {
		return container;
	}

	public NodeFieldContainer getContent() {
		return content;
	}
}
