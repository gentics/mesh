package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

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

//	@GremlinGroovy(value = "it.in('HAS_USER').order({ it.b.getProperty(fieldName) <=> it.a.getProperty(fieldName) })[skip..limit]")
//	public Iterable<User> getUsersInOrder(@GremlinParam("fieldName") String fieldName, @GremlinParam("skip") int skip,
//			@GremlinParam("limit") int limit) {
//		in(AuthRelationships.HAS_USER).order()
//	}

	// @Adjacency(label = AuthRelationships.HAS_USER, direction = Direction.IN)
	public void addUser(User user) {

	}

	public void removeUser(User user) {
		unlinkIn(user, AuthRelationships.HAS_USER);
	}

	public List<Role> getRoles() {
		return in(AuthRelationships.HAS_ROLE).toList(Role.class);
	}

	// @Adjacency(label = AuthRelationships.HAS_ROLE, direction = Direction.IN)
	public void addRole(Role role) {

	}

	public void removeRole(Role role) {
		unlinkIn(role, AuthRelationships.HAS_ROLE);
	}

	// TODO add java handler
	public boolean hasRole(Role extraRole) {
		//TODO this is not optimal - research a better way
		return in(AuthRelationships.HAS_ROLE).toList(Role.class).contains(extraRole);
	}

	// TODO add java handler
	public boolean hasUser(User extraUser) {
		//TODO this is not optimal - research a better way
		return in(AuthRelationships.HAS_USER).toList(Role.class).contains(extraUser);
	}

}
