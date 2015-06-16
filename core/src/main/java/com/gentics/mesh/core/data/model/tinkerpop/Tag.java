package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public class Tag extends GenericPropertyContainer {

	public List<? extends MeshNode> getNodes() {
		return in(MeshRelationships.HAS_TAG).toList(MeshNode.class);
	}

	public void removeNode(MeshNode node) {
		unlinkIn(node, MeshRelationships.HAS_TAG);
	}


}
