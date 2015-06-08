package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.annotations.gremlin.GremlinGroovy;
import com.tinkerpop.frames.annotations.gremlin.GremlinParam;

public interface Group extends GenericNode {

	@Property("name")
	public String getName();

	@Property("name")
	public void setName(String name);

	@Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.IN)
	public Iterable<User> getUsers();

	@GremlinGroovy(value = "it.in('HAS_USER').order({ it.b.getProperty(fieldName) <=> it.a.getProperty(fieldName) })[skip..limit]")
	public Iterable<User> getUsersInOrder(@GremlinParam("fieldName") String fieldName, @GremlinParam("skip") int skip,
			@GremlinParam("limit") int limit);

	@Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.IN)
	public void addUser(User user);

	@Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.IN)
	public void removeUser(User user);

	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public Iterable<Role> getRoles();

	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public void addRole(Role role);

	@Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public void removeRole(Role role);

	//TODO add java handler
	public boolean hasRole(Role extraRole);

	//TODO add java handler
	public boolean hasUser(User extraUser);

}
