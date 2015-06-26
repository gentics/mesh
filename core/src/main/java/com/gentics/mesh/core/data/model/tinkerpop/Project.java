package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;

public class Project extends GenericNode {

	// TODO index to name + unique constraint
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public TagFamilyRoot getTagFamilyRoot() {
		return out(HAS_TAGFAMILY_ROOT).has(TagFamilyRoot.class).nextOrDefaultExplicit(TagFamilyRoot.class, null);
	}

	public void setTagFamilyRoot(TagFamilyRoot root) {
		outE(HAS_TAGFAMILY_ROOT).removeAll();
		linkOut(root, HAS_TAGFAMILY_ROOT);
	}

	public TagFamilyRoot createTagFamilyRoot() {
		TagFamilyRoot root = getGraph().addFramedVertex(TagFamilyRoot.class);
		setTagFamilyRoot(root);
		return root;
	}

	public SchemaRoot getSchemaRoot() {
		return out(HAS_SCHEMA_ROOT).has(SchemaRoot.class).nextOrDefault(SchemaRoot.class, null);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot, HAS_SCHEMA_ROOT);
	}

	public MeshNode getRootNode() {
		return out(HAS_ROOT_NODE).has(MeshNode.class).nextOrDefault(MeshNode.class, null);
	}

	public void setRootNode(MeshNode rootNode) {
		linkOut(rootNode, HAS_ROOT_NODE);
	}

	public ProjectResponse transformToRest(MeshUser user) {
		ProjectResponse projectResponse = new ProjectResponse();
		projectResponse.setUuid(getUuid());
		projectResponse.setName(getName());
		projectResponse.setPermissions(user.getPermissionNames(this));

		// if (rootNode != null) {
		// projectResponse.setRootNodeUuid(rootNode.getUuid());
		// } else {
		// log.info("Inconsistency detected. Project {" + project.getUuid() + "} has no root node.");
		// }
		// return projectResponse;
		return null;
	}

	public MeshNode getOrCreateRootNode() {
		MeshNode rootNode = getRootNode();
		if (rootNode == null) {
			rootNode = getGraph().addFramedVertex(MeshNode.class);
			setRootNode(rootNode);
		}
		return rootNode;

	}

	public void delete() {
		//TODO handle this correctly
		getVertex().remove();
	}

}
