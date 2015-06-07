package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPMeshNode extends TPGenericPropertyContainer {

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.OUT)
	public Iterable<TPTag> getTags();

	@Adjacency(label = BasicRelationships.HAS_PARENT_NODE, direction = Direction.IN)
	public Iterable<TPMeshNode> getChildren();

	@Adjacency(label = BasicRelationships.HAS_PARENT_NODE, direction = Direction.OUT)
	public TPMeshNode getParentNode();

}
