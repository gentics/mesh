package com.gentics.cailun.core.rest.model.auth;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NoArgsConstructor
@Data
@NodeEntity
public class Group extends CaiLunNode {

	private static final long serialVersionUID = -6423363555276535419L;

	@Indexed
	private String name;

	@Fetch
	@RelatedTo(type = AuthRelationships.MEMBER_OF, direction = Direction.INCOMING, elementClass = User.class)
	private Set<User> members = new HashSet<>();

	@Fetch
	@RelatedTo(type = AuthRelationships.HAS_ROLE, direction = Direction.INCOMING, elementClass = Role.class)
	private Set<Role> roles = new HashSet<>();

	@Fetch
	@RelatedTo(type = BasicRelationships.PARENT_OF, direction = Direction.OUTGOING, elementClass = Group.class)
	private Set<Group> parents = new HashSet<>();

	public Group(String name) {
		this.name = name;
	}
}
