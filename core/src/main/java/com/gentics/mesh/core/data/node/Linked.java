package com.gentics.mesh.core.data.node;

import com.syncleus.ferma.AbstractEdgeFrame;

public class Linked extends AbstractEdgeFrame {

	public Node getStartNode() {
		return inV().nextOrDefault(Node.class,null);
	}

	public Node getEndNode() {
		return outV().nextOrDefault(Node.class,null);
	}

}
