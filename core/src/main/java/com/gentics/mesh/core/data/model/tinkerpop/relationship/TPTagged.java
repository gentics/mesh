package com.gentics.mesh.core.data.model.tinkerpop.relationship;

import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.syncleus.ferma.AbstractEdgeFrame;

public class TPTagged extends AbstractEdgeFrame{

	public Tag getStartTag() {
		return inV().next(Tag.class);
	}

	public MeshNode getEndNode() {
		return outV().next(MeshNode.class);
	}
}
