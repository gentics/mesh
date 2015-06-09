package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.ObjectSchemaRoot;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;
import com.tinkerpop.blueprints.Direction;

public class Project extends GenericNode {

	//TODO index to name + unique constraint
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public List<TagFamilyRoot> getTagFamilies() {
		out(BasicRelationships.HAS_TAG_ROOT).toList(TagFamilyRoot.class);
	}

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public ObjectSchemaRoot getSchemaRoot() {
	}

//	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public ObjectSchemaRoot setSchemaRoot(ObjectSchema schemaRoot) {
		
	}

//	@Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
	public MeshRoot getRootNode() {
		
	}

//	@Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
	public void setRootNode(MeshNode rootNode) {
		
	}

//	@Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
	public void setRootNode(MeshRoot root){ 
		
	}

}
