package com.gentics.mesh.core.data.model.auth;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.generic.GenericNode;

@NodeEntity
public class Group extends GenericNode {

	private static final long serialVersionUID = -6423363555276535419L;

	@Indexed(unique = true)
	private String name;

	@RelatedTo(type = AuthRelationships.MEMBER_OF, direction = Direction.INCOMING, elementClass = User.class)
	private Set<User> users = new HashSet<>();

	@RelatedTo(type = AuthRelationships.HAS_ROLE, direction = Direction.INCOMING, elementClass = Role.class)
	private Set<Role> roles = new HashSet<>();

	@SuppressWarnings("unused")
	private Group() {
	}

	public Group(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean addUser(User user) {
		return users.add(user);
	}

	public Set<User> getUsers() {
		return users;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public boolean addRole(Role role) {
		return roles.add(role);
	}

	public boolean removeRole(Role role) {
		return roles.remove(role);
	}

	public boolean removeUser(User user) {
		return users.remove(user);
	}

	public boolean hasUser(User user) {
		// TODO maybe a dedicated cypher statement would be faster?
		return users.contains(user);
	}

	public boolean hasRole(Role role) {
		return this.roles.contains(role);
	}

}
