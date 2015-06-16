package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public class MeshRoot extends MeshVertex {

	public UserRoot getUserRoot() {
		return out(MeshRelationships.HAS_USER_ROOT).next(UserRoot.class);
	}

	public void setUserRoot(UserRoot userRoot) {
		linkOut(userRoot, MeshRelationships.HAS_USER_ROOT);
	}

	public RoleRoot getRoleRoot() {
		return out(MeshRelationships.HAS_ROLE_ROOT).next(RoleRoot.class);
	}

	public void setRoleRoot(RoleRoot roleRoot) {
		linkOut(roleRoot, MeshRelationships.HAS_ROLE_ROOT);
	}

	public GroupRoot getGroupRoot() {
		return out(MeshRelationships.HAS_GROUP_ROOT).next(GroupRoot.class);
	}

	public void setGroupRoot(GroupRoot groupRoot) {
		linkOut(groupRoot, MeshRelationships.HAS_GROUP_ROOT);
	}

	public SchemaRoot getObjectSchemaRoot() {
		return out(MeshRelationships.HAS_SCHEMA_ROOT).next(SchemaRoot.class);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot, MeshRelationships.HAS_SCHEMA_ROOT);
	}

	public LanguageRoot getLanguageRoot() {
		return out(MeshRelationships.HAS_LANGUAGE_ROOT).next(LanguageRoot.class);
	}

	public void setLanguageRoot(LanguageRoot languageRoot) {
		linkOut(languageRoot, MeshRelationships.HAS_LANGUAGE_ROOT);
	}

	public ProjectRoot getProjectRoot() {
		return out(MeshRelationships.HAS_PROJECT_ROOT).next(ProjectRoot.class);
	}

	public void setProjectRoot(ProjectRoot projectRoot) {
		linkOut(projectRoot, MeshRelationships.HAS_PROJECT_ROOT);
	}

}
