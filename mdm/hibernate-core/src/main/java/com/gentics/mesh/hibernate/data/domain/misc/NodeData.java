package com.gentics.mesh.hibernate.data.domain.misc;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.hibernate.data.domain.HibBranchNodeParent;

/**
 * The most fulfilling node data, collected in a single data object.
 * 
 * @author plyhun
 *
 */
public class NodeData {

	private final HibNode node;
	private final HibBranchNodeParent parentEdge;
	private final HibNodeFieldContainerEdge container;
	private final HibNodeFieldContainer content;

	public NodeData(HibNode node, HibBranchNodeParent parentEdge, HibNodeFieldContainerEdge container, HibNodeFieldContainer content) {
		super();
		this.node = node;
		this.parentEdge = parentEdge;
		this.container = container;
		this.content = content;
	}

	public HibNode getNode() {
		return node;
	}

	public HibBranchNodeParent getParentEdge() {
		return parentEdge;
	}

	public HibNodeFieldContainerEdge getContainer() {
		return container;
	}

	public HibNodeFieldContainer getContent() {
		return content;
	}
}
