package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface TPRole extends TPGenericNode {

	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

	@Adjacency(label = AuthRelationships.HAS_PERMISSION, direction = Direction.OUT)
	public Iterable<TPGraphPermission> getPermissions();

	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.OUT)
	public Iterable<TPGroup> getGroups();

}
