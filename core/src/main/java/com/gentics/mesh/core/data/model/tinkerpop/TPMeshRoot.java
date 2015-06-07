package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPMeshRoot extends TPAbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_USER_ROOT, direction = Direction.OUT)
	public TPUserRoot getUserRoot();

	@Adjacency(label = BasicRelationships.HAS_ROLE_ROOT, direction = Direction.OUT)
	public TPRoleRoot getRoleRoot();

	@Adjacency(label = BasicRelationships.HAS_GROUP_ROOT, direction = Direction.OUT)
	public TPGroupRoot getGroupRoot();

	@Adjacency(label = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUT)
	public TPObjectSchemaRoot getObjectSchemaRoot();

	@Adjacency(label = BasicRelationships.HAS_LANGUAGE_ROOT, direction = Direction.OUT)
	public TPLanguageRoot getLanguageRoot();

	@Adjacency(label = BasicRelationships.HAS_PROJECT_ROOT, direction = Direction.OUT)
	public TPProjectRoot getProjectRoot();
}
