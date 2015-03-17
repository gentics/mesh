package com.gentics.cailun.core.data.model.auth;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class Group extends GenericNode {

	private static final long serialVersionUID = -6423363555276535419L;

	@Indexed(unique = true)
	private String name;

	@Fetch
	@RelatedTo(type = AuthRelationships.MEMBER_OF, direction = Direction.INCOMING, elementClass = User.class)
	private Set<User> users = new HashSet<>();

	@Fetch
	@RelatedTo(type = AuthRelationships.HAS_ROLE, direction = Direction.INCOMING, elementClass = Role.class)
	private Set<Role> roles = new HashSet<>();

	@Fetch
	@RelatedTo(type = BasicRelationships.PARENT_OF, direction = Direction.OUTGOING, elementClass = Group.class)
	private Set<Group> parents = new HashSet<>();

	// TODO we are loading the children and the parents. This means we are loading the whole hierarchy when loading one group. Not so good..
	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_SUB_GROUP, direction = Direction.INCOMING, elementClass = Group.class)
	private Set<Group> children = new HashSet<>();

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

	public boolean addGroup(Group group) {
		return this.children.add(group);
	}

	public Set<Group> getGroups() {
		return children;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public Set<Group> getParents() {
		return parents;
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

	public boolean removeGroup(Group group) {
		return this.children.remove(group);

	}

}
