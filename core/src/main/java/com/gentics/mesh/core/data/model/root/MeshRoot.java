package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public class MeshRoot extends MeshVertex {

	public UserRoot getUserRoot() {
		return out(MeshRelationships.HAS_USER_ROOT).nextOrDefault(UserRoot.class, null);
	}

	public void setUserRoot(UserRoot userRoot) {
		linkOut(userRoot, MeshRelationships.HAS_USER_ROOT);
	}

	public RoleRoot getRoleRoot() {
		return out(MeshRelationships.HAS_ROLE_ROOT).nextOrDefault(RoleRoot.class, null);
	}

	public void setRoleRoot(RoleRoot roleRoot) {
		linkOut(roleRoot, MeshRelationships.HAS_ROLE_ROOT);
	}

	public GroupRoot getGroupRoot() {
		return out(MeshRelationships.HAS_GROUP_ROOT).nextOrDefault(GroupRoot.class, null);
	}

	public void setGroupRoot(GroupRoot groupRoot) {
		linkOut(groupRoot, MeshRelationships.HAS_GROUP_ROOT);
	}

	public SchemaRoot getObjectSchemaRoot() {
		return out(MeshRelationships.HAS_SCHEMA_ROOT).nextOrDefault(SchemaRoot.class, null);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot, MeshRelationships.HAS_SCHEMA_ROOT);
	}

	public LanguageRoot getLanguageRoot() {
		return out(MeshRelationships.HAS_LANGUAGE_ROOT).nextOrDefault(LanguageRoot.class, null);
	}

	public void setLanguageRoot(LanguageRoot languageRoot) {
		linkOut(languageRoot, MeshRelationships.HAS_LANGUAGE_ROOT);
	}

	public ProjectRoot getProjectRoot() {
		return out(MeshRelationships.HAS_PROJECT_ROOT).nextOrDefault(ProjectRoot.class, null);
	}

	public void setProjectRoot(ProjectRoot projectRoot) {
		linkOut(projectRoot, MeshRelationships.HAS_PROJECT_ROOT);
	}

}
