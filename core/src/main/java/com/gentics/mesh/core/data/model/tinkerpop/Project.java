package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.ObjectSchemaRoot;
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

	public ObjectSchemaRoot getSchemaRoot() {
		return out(BasicRelationships.HAS_SCHEMA_ROOT).next(ObjectSchemaRoot.class);
	}

	public void setSchemaRoot(ObjectSchemaRoot schemaRoot) {
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
