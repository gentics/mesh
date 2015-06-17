package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.util.TraversalHelper.nextOrNull;

import com.syncleus.ferma.AbstractEdgeFrame;

public class Linked extends AbstractEdgeFrame {

	public MeshNode getStartNode() {
		return nextOrNull(inV(), MeshNode.class);
	}

	public MeshNode getEndNode() {
		return nextOrNull(outV(), MeshNode.class);
	}

}
