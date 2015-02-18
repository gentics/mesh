package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.model.generic.AbstractPersistable;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NodeEntity
public class CaiLunRoot extends AbstractPersistable {

	private static final long serialVersionUID = -901251232180415110L;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_PROJECT, direction = Direction.OUTGOING, elementClass = Project.class)
	Set<Project> projects = new HashSet<>();

	@Fetch
	@RelatedTo(type = AuthRelationships.HAS_USER, direction = Direction.INCOMING, elementClass = User.class)
	private Set<User> users = new HashSet<>();

	@Fetch
	@RelatedTo(type = AuthRelationships.HAS_ROLE, direction = Direction.INCOMING, elementClass = Role.class)
	private Set<Role> roles = new HashSet<>();

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_ROOT_GROUP, direction = Direction.INCOMING, elementClass = Group.class)
	private Group rootGroup;

	public Set<User> getUsers() {
		return users;
	}

	public void setRootGroup(Group group) {
		this.rootGroup = group;
	}

	public Group getRootGroup() {
		return rootGroup;
	}

}
