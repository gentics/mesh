package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface MeshNode extends GenericPropertyContainer {

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.OUT)
	public Iterable<Tag> getTags();

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.OUT)
	public void addTag(Tag tag);

	@Adjacency(label = BasicRelationships.HAS_TAG, direction = Direction.OUT)
	public void removeTag(Tag tag);

	@Adjacency(label = BasicRelationships.HAS_PARENT_NODE, direction = Direction.IN)
	public Iterable<MeshNode> getChildren();

	@Adjacency(label = BasicRelationships.HAS_PARENT_NODE, direction = Direction.OUT)
	public MeshNode getParentNode();

	@Adjacency(label = BasicRelationships.HAS_PARENT_NODE, direction = Direction.OUT)
	public void setParentNode(MeshNode parent);

}
