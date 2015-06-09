package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.User;

public class MeshRoot extends AbstractPersistable {

	// @Adjacency(label = BasicRelationships.HAS_USER_ROOT, direction = Direction.OUT)
	public UserRoot getUserRoot() {
		return out(BasicRelationships.HAS_USER_ROOT).next(UserRoot.class);
	}

	// @Adjacency(label = BasicRelationships.HAS_USER_ROOT, direction = Direction.OUT)
	public void setUserRoot(UserRoot userRoot) {

	}

	// @Adjacency(label = BasicRelationships.HAS_USER_ROOT, direction = Direction.OUT)
	public void addUser(User user) {

	}

	// @Adjacency(label = BasicRelationships.HAS_ROLE_ROOT, direction = Direction.OUT)
	public RoleRoot getRoleRoot() {
		return out(BasicRelationships.HAS_ROLE_ROOT).next(RoleRoot.class);
	}

	// @Adjacency(label = BasicRelationships.HAS_ROLE_ROOT, direction = Direction.OUT)
	public void setRoleRoot(RoleRoot roleRoot) {

	}

	// @Adjacency(label = BasicRelationships.HAS_GROUP_ROOT, direction = Direction.OUT)
	public GroupRoot getGroupRoot() {
		return out(BasicRelationships.HAS_GROUP_ROOT).next(GroupRoot.class);
	}

	// @Adjacency(label = BasicRelationships.HAS_GROUP_ROOT, direction = Direction.OUT)
	public void setGroupRoot(GroupRoot groupRoot) {

	}

	// @Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public ObjectSchemaRoot getObjectSchemaRoot() {
		return out(BasicRelationships.HAS_SCHEMA_ROOT).next(ObjectSchemaRoot.class);
	}

	// @Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public void setObjectSchemaRoot(ObjectSchemaRoot objectSchemaRoot) {

	}

	// @Adjacency(label = BasicRelationships.HAS_LANGUAGE_ROOT, direction = Direction.OUT)
	public LanguageRoot getLanguageRoot() {
		return out(BasicRelationships.HAS_LANGUAGE_ROOT).next(LanguageRoot.class);
	}

	// @Adjacency(label = BasicRelationships.HAS_LANGUAGE_ROOT, direction = Direction.OUT)
	public void setLanguageRoot(LanguageRoot languageRoot) {

	}

	// @Adjacency(label = BasicRelationships.HAS_PROJECT_ROOT, direction = Direction.OUT)
	public ProjectRoot getProjectRoot() {
		return out(BasicRelationships.HAS_PROJECT_ROOT).next(ProjectRoot.class);
	}

	// @Adjacency(label = BasicRelationships.HAS_PROJECT_ROOT, direction = Direction.OUT)
	public void setProjectRoot(ProjectRoot projectRoot) {

	}

}
