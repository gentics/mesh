package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface Tag extends GenericPropertyContainer {

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.IN)
	public Iterable<MeshNode> getNodes();

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.IN)
	public void removeNode(MeshNode node);

}
