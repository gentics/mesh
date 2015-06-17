package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAG_ROOT;

import java.util.List;

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

	public List<? extends TagFamilyRoot> getTagFamilies() {
		return out(HAS_TAG_ROOT).has(TagFamilyRoot.class).toListExplicit(TagFamilyRoot.class);
	}

	public SchemaRoot getSchemaRoot() {
		return out(HAS_SCHEMA_ROOT).nextOrDefault(SchemaRoot.class, null);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot, HAS_SCHEMA_ROOT);
	}

	public MeshNode getRootNode() {
		return out(HAS_ROOT_NODE).nextOrDefault(MeshNode.class, null);
	}

	public void setRootNode(MeshNode rootNode) {
		linkOut(rootNode, HAS_ROOT_NODE);
	}

	public ProjectResponse transformToRest(MeshUser user) {
		ProjectResponse projectResponse = new ProjectResponse();
		projectResponse.setUuid(getUuid());
		projectResponse.setName(getName());
		projectResponse.setPerms(user.getPermissions(this));

		// MeshNode rootNode = neo4jTemplate.fetch(project.getRootNode());
		// if (rootNode != null) {
		// projectResponse.setRootNodeUuid(rootNode.getUuid());
		// } else {
		// log.info("Inconsistency detected. Project {" + project.getUuid() + "} has no root node.");
		// }
		// return projectResponse;
		return null;
	}

	// @Override
	// public Project save(Project project) {
	// ProjectRoot root = projectRepository.findRoot();
	// if (root == null) {
	// throw new NullPointerException("The project root node could not be found.");
	// }
	// project = neo4jTemplate.save(project);
	// root.getProjects().add(project);
	// neo4jTemplate.save(root);
	// return project;
	// return null;
	// }

}
