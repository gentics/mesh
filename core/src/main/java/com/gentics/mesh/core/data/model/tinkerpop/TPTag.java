package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPTag extends TPGenericPropertyContainer {

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.IN)
	public Iterable<TPMeshNode> getNodes();

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.IN)
	public void removeNode(TPMeshNode node);

}
