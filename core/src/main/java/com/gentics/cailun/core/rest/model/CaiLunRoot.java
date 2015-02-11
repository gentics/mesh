package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.auth.User;

@NodeEntity
public class CaiLunRoot extends AbstractPersistable {

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_PROJECT, direction = Direction.OUTGOING, elementClass = Project.class)
	Set<Project> projects = new HashSet<>();

	@Fetch
	@RelatedTo(type = "HAS_MEMBER", direction = Direction.INCOMING, elementClass = User.class)
	private Set<User> members = new HashSet<>();

	@Fetch
	@RelatedTo(type = "HAS_ROLE", direction = Direction.INCOMING, elementClass = Role.class)
	private Set<Role> roles = new HashSet<>();

	@Fetch
	@RelatedTo(type = "HAS_ROOT_GROUP", direction = Direction.INCOMING, elementClass = Group.class)
	private Group rootGroup;

}
