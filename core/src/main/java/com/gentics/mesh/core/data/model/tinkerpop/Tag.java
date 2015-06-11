package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

public class Tag extends GenericPropertyContainer {

	public List<MeshNode> getNodes() {
		return in(BasicRelationships.HAS_TAG).toList(MeshNode.class);
	}

	public void removeNode(MeshNode node) {
		unlinkIn(node, BasicRelationships.HAS_TAG);
	}


}
