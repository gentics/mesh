package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;

public class Project extends GenericNode {

	// TODO index to name + unique constraint
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public List<TagFamilyRoot> getTagFamilies() {
		return out(BasicRelationships.HAS_TAG_ROOT).toList(TagFamilyRoot.class);
	}

	public SchemaRoot getSchemaRoot() {
		return out(BasicRelationships.HAS_SCHEMA_ROOT).next(SchemaRoot.class);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot, BasicRelationships.HAS_SCHEMA_ROOT);
	}

	public MeshNode getRootNode() {
		return out(BasicRelationships.HAS_ROOT_NODE).next(MeshNode.class);
	}

	public void setRootNode(MeshNode rootNode) {
		linkOut(rootNode, BasicRelationships.HAS_ROOT_NODE);
	}

	// @Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
//	public void setRootNode(MeshRoot root) {
//		
//	}

}
