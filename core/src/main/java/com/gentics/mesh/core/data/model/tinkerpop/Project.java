package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
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

	public List<? extends TagFamilyRoot> getTagFamilies() {
		return out(MeshRelationships.HAS_TAG_ROOT).toList(TagFamilyRoot.class);
	}

	public SchemaRoot getSchemaRoot() {
		return out(MeshRelationships.HAS_SCHEMA_ROOT).next(SchemaRoot.class);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot, MeshRelationships.HAS_SCHEMA_ROOT);
	}

	public MeshNode getRootNode() {
		return out(MeshRelationships.HAS_ROOT_NODE).next(MeshNode.class);
	}

	public void setRootNode(MeshNode rootNode) {
		linkOut(rootNode, MeshRelationships.HAS_ROOT_NODE);
	}

	// @Adjacency(label = BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUT)
//	public void setRootNode(MeshRoot root) {
//		
//	}

}
