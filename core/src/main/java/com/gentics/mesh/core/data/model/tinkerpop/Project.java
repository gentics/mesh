package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.ObjectSchemaRoot;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface Project extends GenericNode {

	//TODO index to name + unique constraint
	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

	@Adjacency(label = BasicRelationships.HAS_TAG_ROOT, direction = Direction.OUT)
	public Iterable<TagFamilyRoot> getTagFamilies();

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public ObjectSchemaRoot getSchemaRoot();

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public ObjectSchemaRoot setSchemaRoot(ObjectSchema schemaRoot);

	@Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
	public MeshRoot getRootNode();

	@Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
	public void setRootNode(MeshNode rootNode);

	@Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
	public void setRootNode(MeshRoot root);

}
