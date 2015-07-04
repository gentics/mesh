package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;

public class ProjectImpl extends AbstractGenericNode implements Project {

	// TODO index to name + unique constraint
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public TagFamilyRoot getTagFamilyRoot() {
		return out(HAS_TAGFAMILY_ROOT).has(TagFamilyRootImpl.class).nextOrDefaultExplicit(TagFamilyRootImpl.class, null);
	}

	public void setTagFamilyRoot(TagFamilyRoot root) {
		outE(HAS_TAGFAMILY_ROOT).removeAll();
		linkOut(root.getImpl(), HAS_TAGFAMILY_ROOT);
	}

	public TagFamilyRoot createTagFamilyRoot() {
		TagFamilyRootImpl root = getGraph().addFramedVertex(TagFamilyRootImpl.class);
		setTagFamilyRoot(root);
		return root;
	}

	public SchemaContainerRoot getSchemaRoot() {
		return out(HAS_SCHEMA_ROOT).has(SchemaContainerRootImpl.class).nextOrDefault(SchemaContainerRootImpl.class, null);
	}

	public void setSchemaRoot(SchemaContainerRoot schemaRoot) {
		linkOut(schemaRoot.getImpl(), HAS_SCHEMA_ROOT);
	}

	public Node getRootNode() {
		return out(HAS_ROOT_NODE).has(NodeImpl.class).nextOrDefault(NodeImpl.class, null);
	}

	public void setRootNode(Node rootNode) {
		linkOut(rootNode.getImpl(), HAS_ROOT_NODE);
	}

	public ProjectResponse transformToRest(MeshAuthUser user) {
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

	public Node getOrCreateRootNode() {
		Node rootNode = getRootNode();
		if (rootNode == null) {
			rootNode = getGraph().addFramedVertex(NodeImpl.class);
			setRootNode(rootNode);
		}
		return rootNode;

	}

	public void delete() {
		// TODO handle this correctly
		getVertex().remove();
	}

	@Override
	public ProjectImpl getImpl() {
		return this;
	}

}
