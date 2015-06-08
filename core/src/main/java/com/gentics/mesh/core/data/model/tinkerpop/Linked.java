package com.gentics.mesh.core.data.model.tinkerpop;

import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.InVertex;
import com.tinkerpop.frames.OutVertex;

public interface Linked  extends EdgeFrame {
	
	@InVertex
	public MeshNode getStartNode();

	@OutVertex
	public MeshNode getEndNode();


}
