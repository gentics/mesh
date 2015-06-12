package com.gentics.mesh.core.data.model.tinkerpop;

import com.syncleus.ferma.AbstractEdgeFrame;

public class Linked extends AbstractEdgeFrame {

	public MeshNode getStartNode() {
		return inV().next(MeshNode.class);
	}

	public MeshNode getEndNode() {
		return outV().next(MeshNode.class);
	}

}
