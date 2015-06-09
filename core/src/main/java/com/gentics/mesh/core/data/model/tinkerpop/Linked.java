package com.gentics.mesh.core.data.model.tinkerpop;

import org.jglue.totorom.FramedEdge;

public class Linked  extends FramedEdge {
	
	@InVertex
	public MeshNode getStartNode();

	@OutVertex
	public MeshNode getEndNode();


}
