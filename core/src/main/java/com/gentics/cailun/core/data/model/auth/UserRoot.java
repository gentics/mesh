package com.gentics.cailun.core.data.model.auth;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class UserRoot extends AbstractPersistable {

	private static final long serialVersionUID = 7304523757550541873L;

	@RelatedTo(type = BasicRelationships.HAS_USER, direction = Direction.OUTGOING, elementClass = User.class)
	private Set<User> users = new HashSet<>();

	@Indexed(unique = true)
	private String unique = UserRoot.class.getSimpleName();
	
	public UserRoot() {
	}

	public Set<User> getUsers() {
		return users;
	}

}
