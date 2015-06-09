package com.gentics.mesh.core.data.model.tinkerpop.relationship;

import org.jglue.totorom.FramedEdge;

import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;

public class TPTagged extends FramedEdge{

	@InVertex
	public Tag getStartTag();

	@OutVertex
	public MeshNode getEndNode();
}
