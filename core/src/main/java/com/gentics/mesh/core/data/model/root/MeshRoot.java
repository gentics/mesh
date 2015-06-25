package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_GROUP_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_LANGUAGE_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_PROJECT_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER_ROOT;

import com.gentics.mesh.core.data.model.generic.MeshVertex;

public class MeshRoot extends MeshVertex {

	public UserRoot getUserRoot() {
		return out(HAS_USER_ROOT).has(UserRoot.class).nextOrDefault(UserRoot.class, null);
	}

	public void setUserRoot(UserRoot userRoot) {
		linkOut(userRoot, HAS_USER_ROOT);
	}

	public RoleRoot getRoleRoot() {
		return out(HAS_ROLE_ROOT).has(RoleRoot.class).nextOrDefault(RoleRoot.class, null);
	}

	public void setRoleRoot(RoleRoot roleRoot) {
		linkOut(roleRoot, HAS_ROLE_ROOT);
	}

	public GroupRoot getGroupRoot() {
		return out(HAS_GROUP_ROOT).has(GroupRoot.class).nextOrDefault(GroupRoot.class, null);
	}

	public void setGroupRoot(GroupRoot groupRoot) {
		linkOut(groupRoot, HAS_GROUP_ROOT);
	}

	public SchemaRoot getSchemaRoot() {
		return out(HAS_SCHEMA_ROOT).has(SchemaRoot.class).nextOrDefault(SchemaRoot.class, null);
	}

	public void setSchemaRoot(SchemaRoot schemaRoot) {
		linkOut(schemaRoot, HAS_SCHEMA_ROOT);
	}

	public LanguageRoot getLanguageRoot() {
		return out(HAS_LANGUAGE_ROOT).has(LanguageRoot.class).nextOrDefault(LanguageRoot.class, null);
	}

	public void setLanguageRoot(LanguageRoot languageRoot) {
		linkOut(languageRoot, HAS_LANGUAGE_ROOT);
	}

	public ProjectRoot getProjectRoot() {
		return out(HAS_PROJECT_ROOT).has(ProjectRoot.class).nextOrDefault(ProjectRoot.class, null);
	}

	public void setProjectRoot(ProjectRoot projectRoot) {
		linkOut(projectRoot, HAS_PROJECT_ROOT);
	}

	public ProjectRoot createProjectRoot() {
		ProjectRoot projectRoot = getGraph().addFramedVertex(ProjectRoot.class);
		setProjectRoot(projectRoot);
		return projectRoot;
	}

	public GroupRoot createGroupRoot() {
		GroupRoot groupRoot = getGraph().addFramedVertex(GroupRoot.class);
		setGroupRoot(groupRoot);
		return groupRoot;
	}

	public RoleRoot createRoleRoot() {
		RoleRoot roleRoot = getGraph().addFramedVertex(RoleRoot.class);
		setRoleRoot(roleRoot);
		return roleRoot;
	}

	public SchemaRoot createRoot() {
		SchemaRoot schemaRoot = getGraph().addFramedVertex(SchemaRoot.class);
		setSchemaRoot(schemaRoot);
		return schemaRoot;
	}

	public UserRoot createUserRoot() {
		UserRoot userRoot = getGraph().addFramedVertex(UserRoot.class);
		setUserRoot(userRoot);
		return userRoot;
	}
	
	
	public LanguageRoot createLanguageRoot() {
		LanguageRoot languageRoot = getGraph().addFramedVertex(LanguageRoot.class);
		setLanguageRoot(languageRoot);
		return languageRoot;
	}


	

}
