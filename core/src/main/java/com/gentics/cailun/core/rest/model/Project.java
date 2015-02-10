package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.auth.User;

@NodeEntity
public class Project extends AbstractPersistable {

	private static final long serialVersionUID = -3565883313897315008L;

	@Fetch
	String name;

	@Fetch
	@RelatedTo(type = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> rootTags = new HashSet<>();

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_USER, direction = Direction.OUTGOING, elementClass = User.class)
	private Set<User> users = new HashSet<>();


}
