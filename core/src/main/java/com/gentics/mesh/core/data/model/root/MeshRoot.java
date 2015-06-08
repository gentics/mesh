package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface MeshRoot extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_USER_ROOT, direction = Direction.OUT)
	public UserRoot getUserRoot();

	@Adjacency(label = BasicRelationships.HAS_USER_ROOT, direction = Direction.OUT)
	public void setUserRoot(UserRoot userRoot);

	@Adjacency(label = BasicRelationships.HAS_USER_ROOT, direction = Direction.OUT)
	public void addUser(User user);

	@Adjacency(label = BasicRelationships.HAS_ROLE_ROOT, direction = Direction.OUT)
	public RoleRoot getRoleRoot();

	@Adjacency(label = BasicRelationships.HAS_ROLE_ROOT, direction = Direction.OUT)
	public void setRoleRoot(RoleRoot roleRoot);

	@Adjacency(label = BasicRelationships.HAS_GROUP_ROOT, direction = Direction.OUT)
	public GroupRoot getGroupRoot();

	@Adjacency(label = BasicRelationships.HAS_GROUP_ROOT, direction = Direction.OUT)
	public void setGroupRoot(GroupRoot groupRoot);

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public ObjectSchemaRoot getObjectSchemaRoot();

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public void setObjectSchemaRoot(ObjectSchemaRoot objectSchemaRoot);

	@Adjacency(label = BasicRelationships.HAS_LANGUAGE_ROOT, direction = Direction.OUT)
	public LanguageRoot getLanguageRoot();

	@Adjacency(label = BasicRelationships.HAS_LANGUAGE_ROOT, direction = Direction.OUT)
	public void setLanguageRoot(LanguageRoot languageRoot);

	@Adjacency(label = BasicRelationships.HAS_PROJECT_ROOT, direction = Direction.OUT)
	public ProjectRoot getProjectRoot();

	@Adjacency(label = BasicRelationships.HAS_PROJECT_ROOT, direction = Direction.OUT)
	public void setProjectRoot(ProjectRoot projectRoot);

}
