package com.gentics.mesh.core.data.model.root.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_GROUP_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_LANGUAGE_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_PROJECT_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER_ROOT;

import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.ProjectRoot;
import com.gentics.mesh.core.data.model.root.RoleRoot;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.root.UserRoot;

public class MeshRootImpl extends MeshVertexImpl implements MeshRoot {

	public UserRoot getUserRoot() {
		return out(HAS_USER_ROOT).has(UserRootImpl.class).nextOrDefault(UserRootImpl.class, null);
	}

	public void setUserRoot(UserRoot userRoot) {
		linkOut(userRoot.getImpl(), HAS_USER_ROOT);
	}

	public RoleRoot getRoleRoot() {
		return out(HAS_ROLE_ROOT).has(RoleRootImpl.class).nextOrDefault(RoleRootImpl.class, null);
	}

	public void setRoleRoot(RoleRoot roleRoot) {
		linkOut(roleRoot.getImpl(), HAS_ROLE_ROOT);
	}

	public GroupRoot getGroupRoot() {
		return out(HAS_GROUP_ROOT).has(GroupRootImpl.class).nextOrDefault(GroupRootImpl.class, null);
	}

	public void setGroupRoot(GroupRoot groupRoot) {
		linkOut(groupRoot.getImpl(), HAS_GROUP_ROOT);
	}

	public SchemaRoot getSchemaRoot() {
		return out(HAS_SCHEMA_ROOT).has(SchemaRootImpl.class).nextOrDefault(SchemaRootImpl.class, null);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot.getImpl(), HAS_SCHEMA_ROOT);
	}

	public LanguageRoot getLanguageRoot() {
		return out(HAS_LANGUAGE_ROOT).has(LanguageRootImpl.class).nextOrDefault(LanguageRootImpl.class, null);
	}

	public void setLanguageRoot(LanguageRoot languageRoot) {
		linkOut(languageRoot.getImpl(), HAS_LANGUAGE_ROOT);
	}

	public ProjectRoot getProjectRoot() {
		return out(HAS_PROJECT_ROOT).has(ProjectRootImpl.class).nextOrDefault(ProjectRootImpl.class, null);
	}

	public void setProjectRoot(ProjectRoot projectRoot) {
		linkOut(projectRoot.getImpl(), HAS_PROJECT_ROOT);
	}

	public ProjectRoot createProjectRoot() {
		ProjectRootImpl projectRoot = getGraph().addFramedVertex(ProjectRootImpl.class);
		setProjectRoot(projectRoot);
		return projectRoot;
	}

	public GroupRoot createGroupRoot() {
		GroupRootImpl groupRoot = getGraph().addFramedVertex(GroupRootImpl.class);
		setGroupRoot(groupRoot);
		return groupRoot;
	}

	public RoleRoot createRoleRoot() {
		RoleRootImpl roleRoot = getGraph().addFramedVertex(RoleRootImpl.class);
		setRoleRoot(roleRoot);
		return roleRoot;
	}

	public SchemaRoot createRoot() {
		SchemaRootImpl schemaRoot = getGraph().addFramedVertex(SchemaRootImpl.class);
		setSchemaRoot(schemaRoot);
		return schemaRoot;
	}

	public UserRoot createUserRoot() {
		UserRootImpl userRoot = getGraph().addFramedVertex(UserRootImpl.class);
		setUserRoot(userRoot);
		return userRoot;
	}

	public LanguageRoot createLanguageRoot() {
		LanguageRootImpl languageRoot = getGraph().addFramedVertex(LanguageRootImpl.class);
		setLanguageRoot(languageRoot);
		return languageRoot;
	}

	@Override
	public MeshRootImpl getImpl() {
		return this;
	}

}
