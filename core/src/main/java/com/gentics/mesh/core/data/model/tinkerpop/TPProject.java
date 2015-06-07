package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.TagFamilyRoot;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface TPProject extends TPGenericNode {

	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

	@Adjacency(label = BasicRelationships.HAS_TAG_ROOT, direction = Direction.OUT)
	public Iterable<TagFamilyRoot> getTagFamilies();

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public TPObjectSchemaRoot getSchemaRoot();

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public TPObjectSchemaRoot setSchemaRoot(TPObjectSchema schemaRoot);

	@Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
	public TPMeshRoot getRootNode();

	@Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
	public void setRootNode();

}
