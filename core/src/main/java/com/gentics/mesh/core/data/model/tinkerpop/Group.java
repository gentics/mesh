package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.generic.GenericNode;

public class Group extends GenericNode {

	public static final String NAME_KEY = "name";

	public String getName() {
		return getProperty(NAME_KEY);
	}

	public void setName(String name) {
		setProperty(NAME_KEY, name);
	}

	public Iterable<User> getUsers() {
		return in(AuthRelationships.HAS_USER).toList(User.class);
	}

	@GremlinGroovy(value = "it.in('HAS_USER').order({ it.b.getProperty(fieldName) <=> it.a.getProperty(fieldName) })[skip..limit]")
	public Iterable<User> getUsersInOrder(@GremlinParam("fieldName") String fieldName, @GremlinParam("skip") int skip,
			@GremlinParam("limit") int limit);

	// @Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.IN)
	public void addUser(User user) {

	}

	// @Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.IN)
	public void removeUser(User user) {

	}

	// @Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public Iterable<Role> getRoles() {

	}

	// @Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public void addRole(Role role) {

	}

	// @Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public void removeRole(Role role) {

	}

	// TODO add java handler
	public boolean hasRole(Role extraRole) {
		
	}

	// TODO add java handler
	public boolean hasUser(User extraUser){
		
	}

}
