package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface TPGroup extends TPGenericNode {

	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

	@Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.IN)
	public Iterable<TPUser> getUsers();

	@Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.IN)
	public void addUser(TPUser user);

	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public Iterable<TPRole> getRoles();

	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public void addRole(TPRole role);

}
