package com.gentics.mesh.core.data.model.node;

import com.syncleus.ferma.AbstractEdgeFrame;

public class Linked extends AbstractEdgeFrame {

	public MeshNode getStartNode() {
		return inV().nextOrDefault(MeshNode.class,null);
	}

	public MeshNode getEndNode() {
		return outV().nextOrDefault(MeshNode.class,null);
	}

}
