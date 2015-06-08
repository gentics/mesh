package com.gentics.mesh.core.data.model.tinkerpop.relationship;

import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.InVertex;
import com.tinkerpop.frames.OutVertex;

public interface TPTagged extends EdgeFrame{

	@InVertex
	public Tag getStartTag();

	@OutVertex
	public MeshNode getEndNode();
}
