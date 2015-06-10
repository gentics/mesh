package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

public class MeshRoot extends MeshVertex {

	public UserRoot getUserRoot() {
		return out(BasicRelationships.HAS_USER_ROOT).next(UserRoot.class);
	}

	public void setUserRoot(UserRoot userRoot) {
		linkOut(userRoot, BasicRelationships.HAS_USER_ROOT);
	}

	public RoleRoot getRoleRoot() {
		return out(BasicRelationships.HAS_ROLE_ROOT).next(RoleRoot.class);
	}

	public void setRoleRoot(RoleRoot roleRoot) {
		linkOut(roleRoot, BasicRelationships.HAS_ROLE_ROOT);
	}

	public GroupRoot getGroupRoot() {
		return out(BasicRelationships.HAS_GROUP_ROOT).next(GroupRoot.class);
	}

	public void setGroupRoot(GroupRoot groupRoot) {
		linkOut(groupRoot, BasicRelationships.HAS_GROUP_ROOT);
	}

	public SchemaRoot getObjectSchemaRoot() {
		return out(BasicRelationships.HAS_SCHEMA_ROOT).next(SchemaRoot.class);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot, BasicRelationships.HAS_SCHEMA_ROOT);
	}

	public LanguageRoot getLanguageRoot() {
		return out(BasicRelationships.HAS_LANGUAGE_ROOT).next(LanguageRoot.class);
	}

	public void setLanguageRoot(LanguageRoot languageRoot) {
		linkOut(languageRoot, BasicRelationships.HAS_LANGUAGE_ROOT);
	}

	public ProjectRoot getProjectRoot() {
		return out(BasicRelationships.HAS_PROJECT_ROOT).next(ProjectRoot.class);
	}

	public void setProjectRoot(ProjectRoot projectRoot) {
		linkOut(projectRoot, BasicRelationships.HAS_PROJECT_ROOT);
	}

}
